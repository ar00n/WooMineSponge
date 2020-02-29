package ar0n.woominesponge;
import ar0n.woominesponge.pojo.Order;
import ar0n.woominesponge.pojo.WMCPojo;
import ar0n.woominesponge.pojo.WMCProcessedOrders;

import ar0n.woominesponge.pojo.WMCPojo;
import ar0n.woominesponge.pojo.WMCProcessedOrders;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import ninja.leaping.configurate.ConfigurationNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.plugin.Plugin;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.scheduler.Task;

import static org.spongepowered.api.Sponge.getGame;
import static org.spongepowered.api.Sponge.getServer;

@Plugin(
        id = "woominesponge",
        name = "WooMineSponge",
        url = "https://ar0n.com",
        authors = {
                "ar0n"
        }
)
public class WooMineSponge {

    @Inject
    public Logger logger;

    static WooMineSponge instance;

    Task.Builder taskBuilder = Task.builder();

    ConfigurationNode rootNode;

    Boolean debug = false;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path defaultConfig;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    @Inject
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Listener
    public void onServerStart(GameStartedServerEvent event) throws IOException {
        instance = this;
        if (!Files.exists(defaultConfig)) {
            Sponge.getAssetManager().getAsset(this, "default.conf").get().copyToFile(defaultConfig);
        }
        rootNode = configManager.load();

        String lang = rootNode.getNode("lang").getString();

        debug = rootNode.getNode("debug").getBoolean();

        // Build the command structure

        // 1) subcommands of the /mail command
        HashMap<List<String>, CommandSpec> subcommands = new HashMap<>();

        // 1a) /mail read
        subcommands.put(Arrays.asList("check"), CommandSpec.builder()
                .permission("woo.admin")
                .description(Text.of("Checks for payments."))
                .executor(new WooCommand()) // <-- command logic is in there
                .build());

        // 2) main command
        CommandSpec mailCommand = CommandSpec
                .builder()
                .description(Text.of("WooMineSponge Command"))
                .children(subcommands) // register subcommands
                .build();

        // Register the mail command
        Sponge.getCommandManager().register(this, mailCommand, "woo");

        // Log when plugin is initialized.
        logger.info("Plugin has been initialised.");

        Task.builder().execute(() -> {
            try {
                check();
            } catch (Exception e) {
                logger.error(e.toString());
                e.printStackTrace();
            }
        })
                .async().delay(100, TimeUnit.MILLISECONDS).interval(rootNode.getNode("interval").getLong(), TimeUnit.SECONDS)
                .name("Process Purchases - Interval").submit(this);

        try {
            validateConfig();
            logger.info("Plugin has been fully enabled.");
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Config error. Disabling plugin.");
            getGame().getEventManager().unregisterPluginListeners(this);
            getGame().getCommandManager().getOwnedBy(this).forEach(getGame().getCommandManager()::removeMapping);
            getGame().getScheduler().getScheduledTasks(this).forEach(Task::cancel);
        }
    }

    /**
     * Gets the site URL
     *
     * @return URL
     * @throws Exception Why the URL failed.
     */
    private URL getSiteURL() throws Exception {
        return new URL( rootNode.getNode("url").getString() + "/wp-json/wmc/v1/server/" + rootNode.getNode("key").getString() );
    }

    boolean check() throws Exception {

        // Make 100% sure the config has at least a key and url
        this.validateConfig();

        // Contact the server.
        String pendingOrders = getPendingOrders();

        // Server returned an empty response, bail here.
        if ( pendingOrders.isEmpty() ) {
            return false;
        }

        // Create new object from JSON response.
        Gson gson = new GsonBuilder().create();
        WMCPojo wmcPojo = gson.fromJson( pendingOrders, WMCPojo.class );
        List<Order> orderList = wmcPojo.getOrders();

        // Log if debugging is enabled.
        wmc_log( pendingOrders );

        // Validate we can indeed process what we need to.
        if ( wmcPojo.getData() != null ) {
            // We have an error, so we need to bail.
            wmc_log( "Code:" + wmcPojo.getCode(), 3 );
            throw new Exception( wmcPojo.getMessage() );
        }

        if ( orderList == null || orderList.isEmpty() ) {
            wmc_log( "No orders to process.", 2 );
            return false;
        }

        // foreach ORDERS in JSON feed
        List<Integer> processedOrders = new ArrayList<>();
        for ( Order order : orderList ) {
            Optional<Player> player = getServer().getPlayer(order.getPlayer());
            if ( null == player ) {
                continue;
            }

            // World whitelisting.
            //if ( getConfig().isSet( "whitelist-worlds" ) ) {
            //    List<String> whitelistWorlds = getConfig().getStringList( "whitelist-worlds" );
            //    String playerWorld = player.getWorld().getName();
            //    if ( ! whitelistWorlds.contains( playerWorld ) ) {
            //        wmc_log( "Player " + player.getDisplayName() + " was in world " + playerWorld + " which is not in the white-list, no commands were ran." );
            //        continue;
            //    }
            //}

            // Walk over all commands and run them at the next available tick.
            for ( String command : order.getCommands() ) {
                Task.builder().execute(() -> Sponge.getCommandManager().process(getServer().getConsole(), command))
                        .async().delayTicks(20L)
                        .submit(this);
            }

            wmc_log( "Adding item to list - " + order.getOrderId() );
            processedOrders.add( order.getOrderId() );
            wmc_log( "Processed length is " + processedOrders.size() );
        }

        // If it's empty, we skip it.
        if ( processedOrders.isEmpty() ) {
            return false;
        }

        // Send/update processed orders.
        return sendProcessedOrders( processedOrders );
    }

    private void validateConfig() throws Exception {

        if ( 1 > rootNode.getNode("url").getString().length() ) {
            throw new Exception( "Server URL is empty, check config." );
        } else if ( rootNode.getNode("url").getString().equals( "https://ar0n.com" ) ) {
            throw new Exception( "URL is still the default URL, check config." );
        } else if ( 1 > rootNode.getNode("key").getString().length() ) {
            throw new Exception( "Server Key is empty, this is insecure, check config." );
        }
    }

    /**
     * Sends the processed orders to the site.
     *
     * @param processedOrders A list of order IDs which were processed.
     * @return boolean
     */
    private boolean sendProcessedOrders( List<Integer> processedOrders ) throws Exception {
        // Build the GSON data to send.
        Gson gson = new Gson();
        WMCProcessedOrders wmcProcessedOrders = new WMCProcessedOrders();
        wmcProcessedOrders.setProcessedOrders( processedOrders );
        String orders = gson.toJson( wmcProcessedOrders );

        // Setup the client.
        OkHttpClient client = new OkHttpClient();

        // Process stuffs now.
        RequestBody body = RequestBody.create( MediaType.parse( "application/json; charset=utf-8" ), orders );
        Request request = new Request.Builder().url( getSiteURL() ).post( body ).build();
        Response response = client.newCall( request ).execute();

        // If the body is empty we can do nothing.
        if ( null == response.body() ) {
            throw new Exception( "Received empty response from your server, check connections." );
        }

        // Get the JSON reply from the endpoint.
        WMCPojo wmcPojo = gson.fromJson( response.body().string(), WMCPojo.class );
        if ( null != wmcPojo.getCode() ) {
            wmc_log( "Received error when trying to send post data:" + wmcPojo.getCode(), 3 );
            throw new Exception( wmcPojo.getMessage() );
        }

        return true;
    }

    /**
     * Gets pending orders from the WordPress JSON endpoint.
     *
     * @return String
     * @throws Exception On failure.
     */
    private String getPendingOrders() throws Exception {
        URL baseURL = getSiteURL();
        BufferedReader in;
        try {
            in = new BufferedReader(new InputStreamReader(baseURL.openStream()));
        } catch( FileNotFoundException e ) {
            String msg = e.getMessage().replace( rootNode.getNode("key").getString(), "privateKey" );
            throw new FileNotFoundException( msg );
        }

        StringBuilder buffer = new StringBuilder();

        // Walk over each line of the response.
        String line;
        while ( ( line = in.readLine() ) != null ) {
            buffer.append( line );
        }

        in.close();

        return buffer.toString();
    }

    /**
     * Log stuffs.
     *
     * @param message The message to log.
     */
    private void wmc_log(String message) {
        this.wmc_log( message, 1 );
    }

    /**
     * Log stuffs.
     *
     * @param message The message to log.
     * @param level The level to log it at.
     */
    private void wmc_log(String message, Integer level) {

        if ( ! debug ) {
            return;
        }

        switch ( level ) {
            case 1:
                logger.info(message);
                break;
            case 2:
                logger.warn(message);
                break;
            case 3:
                logger.error(message);
                break;
        }
    }
}

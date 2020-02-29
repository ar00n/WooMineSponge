package ar0n.woominesponge;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.selector.Argument;

public class WooCommand implements CommandExecutor {
    @Inject
    public void setLogger(Logger logger) {
        WooMineSponge.instance.logger = logger;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        try {
            String msg;
            boolean checkResults = WooMineSponge.instance.check();

            if ( !checkResults ) {
                src.sendMessage(Text.of("No purchases available - please try again soon."));
            } else {
                src.sendMessage(Text.of("All purchases processed."));
            }
        } catch ( Exception e ) {
            WooMineSponge.instance.logger.warn(e.getMessage());

            e.printStackTrace();
        }
        return CommandResult.success();
    }
}
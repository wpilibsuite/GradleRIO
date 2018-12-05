package edu.wpi.first.toolchain.roborio;

import edu.wpi.first.toolchain.GccToolChain;
import edu.wpi.first.toolchain.ToolchainOptions;

public class RoboRioGcc extends GccToolChain {

    public RoboRioGcc(ToolchainOptions options) {
        super(options);
    }

    @Override
    protected String getTypeName() {
        return "RoboRioGcc";
    }
}

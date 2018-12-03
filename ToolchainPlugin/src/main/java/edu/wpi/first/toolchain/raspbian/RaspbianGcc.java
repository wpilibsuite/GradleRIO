package edu.wpi.first.toolchain.raspbian;

import edu.wpi.first.toolchain.GccToolChain;
import edu.wpi.first.toolchain.ToolchainOptions;

public class RaspbianGcc extends GccToolChain {

    public RaspbianGcc(ToolchainOptions options) {
        super(options);
    }

    @Override
    protected String getTypeName() {
        return "RaspbianGcc";
    }
}

package com.renomad;

import minum.Constants;
import minum.logging.Logger;
import minum.web.FullSystem;

public class Main {

    public static void main(String[] args) {
        // Start the system
        FullSystem fs = FullSystem.initialize();

        // Register some endpoints
        new TheRegister(fs.getContext()).registerDomains();

        fs.block();
    }
}

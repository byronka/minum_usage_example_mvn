package com.renomad;

import com.renomad.minum.state.Constants;
import com.renomad.minum.logging.Logger;
import com.renomad.minum.web.FullSystem;

public class Main {

    public static void main(String[] args) {
        // Start the system
        FullSystem fs = FullSystem.initialize();

        // Register some endpoints
        new TheRegister(fs.getContext()).registerDomains();

        fs.block();
    }
}

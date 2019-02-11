/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data;

/**
 * Created by Macsleven on 05/01/2018.
 */

public class Connection {
    private String connection;

    public Connection() {
    }

    public Connection(String connection) {
        this.connection = connection;
    }

    public String getConnection() {
        return connection;
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }
}

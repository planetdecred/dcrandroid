package com.decrediton.data;

/**
 * Created by Macsleven on 05/01/2018.
 */

public class Connection{
        private String connection;
        public Connection(){
        }
        public Connection(String connection){
            this.connection = connection;
        }

        public String getConnection(){
            return  connection;
        }

        public void setConnection(String seed){
            this.connection = connection;
        }
}

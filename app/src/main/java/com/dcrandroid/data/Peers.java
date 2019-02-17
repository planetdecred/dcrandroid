/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data;

/**
 * Created by Macsleven on 05/01/2018.
 */

public class Peers {
    private String id, addr, addrlocal, services, relaytxes,
            lastsend, lastrecv, bytessent, bytesrecv, conntime,
            timeoffset, pingtime, version, subver, inbound, startingheight,
            currentheight, banscore, syncode;

    public Peers(String id, String addr, String addrlocal, String services, String relaytxes, String lastsend, String lastrecv, String bytessent, String bytesrecv, String conntime, String timeoffset, String pingtime, String version, String subver, String inbound, String startingheight, String currentheight, String banscore, String syncode) {
        this.id = id;
        this.addr = addr;
        this.addrlocal = addrlocal;
        this.services = services;
        this.relaytxes = relaytxes;
        this.lastsend = lastsend;
        this.lastrecv = lastrecv;
        this.bytessent = bytessent;
        this.bytesrecv = bytesrecv;
        this.conntime = conntime;
        this.timeoffset = timeoffset;
        this.pingtime = pingtime;
        this.version = version;
        this.subver = subver;
        this.inbound = inbound;
        this.startingheight = startingheight;
        this.currentheight = currentheight;
        this.banscore = banscore;
        this.syncode = syncode;
    }

    public Peers() {
    }

    public String getTimeoffset() {
        return timeoffset;
    }

    public void setTimeoffset(String timeoffset) {
        this.timeoffset = timeoffset;
    }

    public String getAddrlocal() {
        return addrlocal;
    }

    public void setAddrlocal(String addrlocal) {
        this.addrlocal = addrlocal;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getRelaytxes() {
        return relaytxes;
    }

    public void setRelaytxes(String relaytxes) {
        this.relaytxes = relaytxes;
    }

    public String getLastsend() {
        return lastsend;
    }

    public void setLastsend(String lastsend) {
        this.lastsend = lastsend;
    }

    public String getLastrecv() {
        return lastrecv;
    }

    public void setLastrecv(String lastrecv) {
        this.lastrecv = lastrecv;
    }

    public String getBytessent() {
        return bytessent;
    }

    public void setBytessent(String bytessent) {
        this.bytessent = bytessent;
    }

    public String getBytesrecv() {
        return bytesrecv;
    }

    public void setBytesrecv(String bytesrecv) {
        this.bytesrecv = bytesrecv;
    }

    public String getConntime() {
        return conntime;
    }

    public void setConntime(String conntime) {
        this.conntime = conntime;
    }

    public String getPingtime() {
        return pingtime;
    }

    public void setPingtime(String pingtime) {
        this.pingtime = pingtime;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSubver() {
        return subver;
    }

    public void setSubver(String subver) {
        this.subver = subver;
    }

    public String getInbound() {
        return inbound;
    }

    public void setInbound(String inbound) {
        this.inbound = inbound;
    }

    public String getStartingheight() {
        return startingheight;
    }

    public void setStartingheight(String startingheight) {
        this.startingheight = startingheight;
    }

    public String getCurrentheight() {
        return currentheight;
    }

    public void setCurrentheight(String currentheight) {
        this.currentheight = currentheight;
    }

    public String getBanscore() {
        return banscore;
    }

    public void setBanscore(String banscore) {
        this.banscore = banscore;
    }

    public String getSyncode() {
        return syncode;
    }

    public void setSyncode(String syncode) {
        this.syncode = syncode;
    }
}

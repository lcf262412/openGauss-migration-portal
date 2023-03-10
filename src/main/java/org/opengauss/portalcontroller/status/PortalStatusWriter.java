package org.opengauss.portalcontroller.status;

/**
 * The type Portal status writer.
 */
public class PortalStatusWriter {
    private int status;
    private long timestamp;

    private String msg;

    /**
     * Gets status.
     *
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets status.
     *
     * @param status the status
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Gets timestamp.
     *
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Sets timestamp.
     *
     * @param timestamp the timestamp
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets msg.
     *
     * @return the msg
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Sets msg.
     *
     * @param msg the msg
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * Instantiates a new Portal status writer.
     *
     * @param status    the status
     * @param timestamp the timestamp
     */
    public PortalStatusWriter(int status, long timestamp) {
        this.status = status;
        this.timestamp = timestamp;
    }

    /**
     * Instantiates a new Portal status writer.
     *
     * @param status    the status
     * @param timestamp the timestamp
     * @param msg       the msg
     */
    public PortalStatusWriter(int status, long timestamp, String msg) {
        this.status = status;
        this.timestamp = timestamp;
        this.msg = msg;
    }
}

package org.opengauss.portalcontroller.status;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.opengauss.portalcontroller.Plan;
import org.opengauss.portalcontroller.PortalControl;
import org.opengauss.portalcontroller.Tools;
import org.opengauss.portalcontroller.constant.Chameleon;
import org.opengauss.portalcontroller.constant.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The type Change status tools.
 */
public class ChangeStatusTools {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeStatusTools.class);

    /**
     * Gets chameleon table status.
     *
     * @return the chameleon table status
     */
    public static ArrayList<TableStatus> getChameleonTableStatus() {
        String chameleonVenvPath = PortalControl.toolsConfigParametersTable.get(Chameleon.VENV_PATH);
        String path = chameleonVenvPath + "data_default_" + Plan.workspaceId + "_init_replica.json";
        ArrayList<TableStatus> tableStatusList = getChameleonTableStatus(path);
        return tableStatusList;
    }

    /**
     * Gets chameleon table status.
     *
     * @param path the path
     * @return the chameleon table status
     */
    public static ArrayList<TableStatus> getChameleonTableStatus(String path) {
        ArrayList<TableStatus> tableStatusList = new ArrayList<>();
        File file = new File(path);
        String tableChameleonStatus = "";
        if (!(tableChameleonStatus = Tools.readFile(file)).equals("")) {
            JSONObject root = JSONObject.parseObject(tableChameleonStatus);
            JSONArray table = root.getJSONArray("table");
            Iterator<Object> iterator = table.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                String name = table.getJSONObject(index).getString("name");
                double percent = table.getJSONObject(index).getDouble("percent");
                int status = table.getJSONObject(index).getInteger("status");
                TableStatus tableStatus = new TableStatus(name, status, percent);
                tableStatusList.add(tableStatus);
                index++;
                iterator.next();
            }
            boolean isFullCheck = PortalControl.status < Status.START_INCREMENTAL_MIGRATION && PortalControl.status > Status.FULL_MIGRATION_FINISHED;
            if (new File(PortalControl.portalWorkSpacePath + "check_result/result").exists() && isFullCheck) {
                tableStatusList = getDatacheckTableStatus(tableStatusList);
            }
        }
        return tableStatusList;
    }

    /**
     * Gets datacheck table status.
     *
     * @param tableStatusArrayList the table status array list
     * @return the datacheck table status
     */
    public static ArrayList<TableStatus> getDatacheckTableStatus(ArrayList<TableStatus> tableStatusArrayList) {
        String successPath = PortalControl.portalWorkSpacePath + "check_result/result/success.log";
        tableStatusArrayList = getDatacheckTableStatus(successPath, tableStatusArrayList);
        return tableStatusArrayList;
    }


    /**
     * Gets chameleon object status.
     *
     * @param name  the name
     * @param order the order
     * @return the chameleon object status
     */
    public static ArrayList<ObjectStatus> getChameleonObjectStatus(String name, String order) {
        String chameleonVenvPath = PortalControl.toolsConfigParametersTable.get(Chameleon.VENV_PATH);
        String path = chameleonVenvPath + "data_default_" + Plan.workspaceId + "_" + order + ".json";
        if (!new File(path).exists()) {
            path = chameleonVenvPath + "data_default_" + Plan.workspaceId + "_init_replica.json";
        }
        File file = new File(path);
        ArrayList<ObjectStatus> objectStatusList = getChameleonObjectStatus(name, file);
        return objectStatusList;
    }

    /**
     * Gets chameleon object status.
     *
     * @param name the name
     * @param file the file
     * @return the chameleon object status
     */
    public static ArrayList<ObjectStatus> getChameleonObjectStatus(String name, File file) {
        ArrayList<ObjectStatus> objectStatusList = new ArrayList<>();
        String chameleonStr = "";
        if (file.exists()) {
            chameleonStr = Tools.readFile(file);
            if (!chameleonStr.equals("")) {
                JSONObject root = JSONObject.parseObject(chameleonStr);
                if (root.getJSONArray(name) != null) {
                    JSONArray objects = root.getJSONArray(name);
                    Iterator iterator = objects.iterator();
                    int index = 0;
                    while (iterator.hasNext()) {
                        String objectName = objects.getJSONObject(index).getString("name");
                        int status = objects.getJSONObject(index).getInteger("status");
                        ObjectStatus objectStatus = new ObjectStatus(objectName, status);
                        objectStatusList.add(objectStatus);
                        index++;
                        iterator.next();
                    }
                }
            }
        }
        return objectStatusList;
    }


    /**
     * Gets all chameleon status.
     *
     * @return the all chameleon status
     */
    public static FullMigrationStatus getAllChameleonStatus() {
        ArrayList<TableStatus> tableStatusArrayList = getChameleonTableStatus();
        ArrayList<ObjectStatus> viewStatusArrayList = getChameleonObjectStatus("view", "start_view_replica");
        ArrayList<ObjectStatus> functionStatusArrayList = getChameleonObjectStatus("function", "start_func_replica");
        ArrayList<ObjectStatus> triggerStatusArrayList = getChameleonObjectStatus("trigger", "start_trigger_replica");
        ArrayList<ObjectStatus> procedureStatusArrayList = getChameleonObjectStatus("procedure", "start_proc_replica");
        FullMigrationStatus fullMigrationStatus = new FullMigrationStatus(tableStatusArrayList, viewStatusArrayList, functionStatusArrayList, triggerStatusArrayList, procedureStatusArrayList);
        return fullMigrationStatus;
    }

    /**
     * Change full status.
     */
    public static void changeFullStatus() {
        FullMigrationStatus tempFullMigrationStatus;
        String fullMigrationStatusString = "";
        try {
            tempFullMigrationStatus = getAllChameleonStatus();
        } catch (JSONException e) {
            tempFullMigrationStatus = ThreadStatusController.fullMigrationStatus;
        }
        ThreadStatusController.fullMigrationStatus = tempFullMigrationStatus;
        fullMigrationStatusString = JSON.toJSONString(ThreadStatusController.fullMigrationStatus);
        Tools.writeFile(fullMigrationStatusString, new File(PortalControl.portalWorkSpacePath + "status/full_migration.txt"), false);
    }

    /**
     * Change incremental status int.
     *
     * @param sourceMigrationStatusPath      the source migration status path
     * @param sinkMigrationStatusPath        the sink migration status path
     * @param incrementalMigrationStatusPath the incremental migration status path
     * @param count                          the count
     * @return the int
     */
    public static int changeIncrementalStatus(String sourceMigrationStatusPath, String sinkMigrationStatusPath, String incrementalMigrationStatusPath, String count) {
        int time = 0;
        String sourceStr = "";
        sourceStr = Tools.readFile(new File(sourceMigrationStatusPath));
        JSONObject sourceObject = JSONObject.parseObject(sourceStr);
        int createCount = sourceObject.getInteger(count);
        int sourceSpeed = sourceObject.getInteger("speed");
        long sourceFirstTimestamp = sourceObject.getLong("timestamp");
        String sinkStr = "";
        sinkStr = Tools.readFile(new File(sinkMigrationStatusPath));
        JSONObject sinkObject = JSONObject.parseObject(sinkStr);
        int replayedCount = sinkObject.getInteger("replayedCount");
        int sinkSpeed = sinkObject.getInteger("speed");
        long sinkTimestamp = sinkObject.getLong("timestamp");
        if (sinkTimestamp > sourceFirstTimestamp) {
            String timeStr = String.valueOf(sourceFirstTimestamp + 1000 - sinkTimestamp);
            time = Integer.parseInt(timeStr);
            sourceStr = Tools.readFile(new File(sourceMigrationStatusPath));
            JSONObject sourceSecondObject = JSONObject.parseObject(sourceStr);
            createCount = sourceSecondObject.getInteger(count);
            sourceSpeed = sourceSecondObject.getInteger("speed");
        }
        int rest = createCount - replayedCount;
        if (time > 1000) {
            time = 1000;
        }
        Tools.sleepThread(time, "writing the status");
        String incrementalMigrationString = "";
        int status = Status.Incremental.RUNNING;
        if (PortalControl.status == Status.ERROR) {
            status = Status.Incremental.ERROR;
            String msg = "error";
            IncrementalMigrationStatus incrementalMigrationStatus = new IncrementalMigrationStatus(status, createCount, sourceSpeed, sinkSpeed, rest, msg);
            incrementalMigrationString = JSON.toJSONString(incrementalMigrationStatus);
        } else {
            IncrementalMigrationStatus incrementalMigrationStatus = new IncrementalMigrationStatus(status, createCount, sourceSpeed, sinkSpeed, rest);
            incrementalMigrationString = JSON.toJSONString(incrementalMigrationStatus);
        }
        Tools.writeFile(incrementalMigrationString, new File(incrementalMigrationStatusPath), false);

        return time;
    }

    /**
     * Write portal status.
     */
    public static void writePortalStatus() {
        try {
            FileWriter fw = new FileWriter(new File(PortalControl.portalWorkSpacePath + "status/portal.txt"));
            PortalStatusWriter portalStatusWriter;
            if (PortalControl.status == Status.ERROR) {
                portalStatusWriter = new PortalStatusWriter(PortalControl.status, System.currentTimeMillis(), PortalControl.errorMsg);
            } else {
                portalStatusWriter = new PortalStatusWriter(PortalControl.status, System.currentTimeMillis());
            }
            ThreadStatusController.portalStatusWriterArrayList.add(portalStatusWriter);
            String str = JSON.toJSONString(ThreadStatusController.portalStatusWriterArrayList);
            fw.write(str);
            fw.flush();
            fw.close();
        } catch (IOException e) {
            LOGGER.error("IOException occurred in writing file " + PortalControl.portalWorkSpacePath + "status/portal.txt" + ".");
        }
    }

    /**
     * Output chameleon table status.
     */
    public static void outputChameleonTableStatus() {
        LOGGER.info("Table:");
        String path = PortalControl.portalWorkSpacePath + "status/full_migration.txt";
        ArrayList<TableStatus> tableStatusArrayList = getChameleonTableStatus(path);
        for (TableStatus tableStatus : tableStatusArrayList) {
            LOGGER.info("Name: " + tableStatus.getName() + ", percent: " + tableStatus.getPercent() + ", status: " + Status.Object.HASHTABLE.get(tableStatus.getStatus()));
        }
    }

    /**
     * Output chameleon object status.
     *
     * @param name the name
     */
    public static void outputChameleonObjectStatus(String name) {
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        LOGGER.info(name + ":");
        String path = PortalControl.portalWorkSpacePath + "status/full_migration.txt";
        File file = new File(path);
        ArrayList<ObjectStatus> tableStatusArrayList = getChameleonObjectStatus(name, file);
        for (ObjectStatus objectStatus : tableStatusArrayList) {
            LOGGER.info("Name: " + objectStatus.getName() + ", status: " + Status.Object.HASHTABLE.get(objectStatus.getStatus()));
        }
    }

    /**
     * Output chameleon status.
     */
    public static void outputChameleonStatus() {
        outputChameleonTableStatus();
        outputChameleonObjectStatus("view");
        outputChameleonObjectStatus("function");
        outputChameleonObjectStatus("trigger");
        outputChameleonObjectStatus("procedure");
    }

    /**
     * Output incremental status.
     *
     * @param path the path
     */
    public static void outputIncrementalStatus(String path) {
        String tempStr = Tools.readFile(new File(path));
        if (!tempStr.equals("")) {
            JSONObject root = JSONObject.parseObject(tempStr);
            int status = root.getInteger("status");
            int count = root.getInteger("count");
            int sourceSpeed = root.getInteger("sourceSpeed");
            int sinkSpeed = root.getInteger("sinkSpeed");
            int rest = root.getInteger("rest");
            String msg = root.getString("msg");
            if (status == Status.Incremental.RUNNING && PortalControl.status == Status.RUNNING_INCREMENTAL_MIGRATION) {
                LOGGER.info("Incremental migration status: running");
            } else if (status == Status.Incremental.RUNNING && PortalControl.status == Status.INCREMENTAL_MIGRATION_FINISHED) {
                LOGGER.info("Incremental migration status: finished");
            } else {
                PortalControl.status = Status.ERROR;
                PortalControl.errorMsg = msg;
                LOGGER.info("Incremental migration status: error, message: " + msg);
            }
            LOGGER.info("Count: " + count + ", sourceSpeed: " + sourceSpeed + ", sinkSpeed: " + sinkSpeed + ", rest: " + rest);
        }
    }

    /**
     * Gets portal status.
     *
     * @param threadStatusController the thread status controller
     * @return the portal status
     */
    public static int getPortalStatus(ThreadStatusController threadStatusController) {
        int status = 0;
        String str = Tools.readFile(new File(PortalControl.portalWorkSpacePath + "status/portal.txt"));
        JSONArray array = JSONArray.parseArray(str);
        Iterator iterator = array.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            status = array.getJSONObject(index).getInteger("status");
            index++;
            iterator.next();
        }
        return status;
    }

    /**
     * Gets datacheck table status.
     *
     * @param path                 the path
     * @param tableStatusArrayList the table status array list
     * @return the datacheck table status
     */
    public static ArrayList<TableStatus> getDatacheckTableStatus(String path, ArrayList<TableStatus> tableStatusArrayList) {
        String str = Tools.readFile(new File(path));
        if (!str.equals("")) {
            str = "[" + str.substring(0, str.length() - 1) + "]";
            JSONArray array = JSONArray.parseArray(str);
            Iterator iterator = array.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                String tableName = array.getJSONObject(index).getString("tableName");
                for (TableStatus tableStatus : tableStatusArrayList) {
                    if (tableStatus.getName().equals(tableName)) {
                        tableStatus.setPercent(1.0);
                        tableStatus.setStatus(Status.Object.FULL_MIGRATION_CHECK_FINISHED);
                        break;
                    }
                }
                index++;
                iterator.next();
            }
        }
        return tableStatusArrayList;
    }
}

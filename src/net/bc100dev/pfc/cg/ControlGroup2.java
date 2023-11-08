package net.bc100dev.pfc.cg;

import net.bc100dev.commons.utils.io.FileUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ControlGroup2 {

    private final String cgName;
    private final Path cgPath;

    private final List<String> cgControlTypes = new ArrayList<>();

    protected ControlGroup2(String cgName, Path cgPath, String... cgControlTypes) {
        this.cgName = cgName;
        this.cgPath = cgPath;

        Collections.addAll(this.cgControlTypes, cgControlTypes);
    }

    private String findPath(String cgLabel, boolean withinTasks) {
        String filePath = cgPath + "/";

        if (withinTasks)
            filePath += "tasks/";

        filePath += cgLabel;
        return filePath;
    }

    public String getName() {
        return cgName;
    }

    public String[] getControlTypes() {
        String[] arr = new String[cgControlTypes.size()];

        for (int i = 0; i < cgControlTypes.size(); i++)
            arr[i] = cgControlTypes.get(i);

        return arr;
    }

    public String[] getAvailableLabels(boolean taskLabels) throws IOException {
        List<String> contents = taskLabels ? FileUtil.listDirectory(cgPath + "/tasks", true, true, false) : FileUtil.listDirectory(cgPath.toString(), true, true, false);
        if (contents.isEmpty())
            return new String[0];

        String[] arr = new String[contents.size()];

        for (int i = 0; i < contents.size(); i++)
            arr[i] = contents.get(i);

        return arr;
    }

    /**
     * Adds the process to the cgroups, giving that directory the use of the current process,
     * being the current process ID.
     *
     * @throws IOException On cgroup or internal write errors
     */
    public void addProcess() throws IOException {
        File procsFile = new File(cgPath + "/tasks/cgroup.procs");
        if (!procsFile.exists())
            throw new IOException("cgroups failed to initialize");

        if (!procsFile.canWrite())
            throw new PermissionDeniedException("cannot write to a protected file as normal user");

        ProcessHandle pHandle = ProcessHandle.current();
        long pid = pHandle.pid();

        FileOutputStream fos = new FileOutputStream(procsFile);
        fos.write(String.valueOf(pid).getBytes());
        fos.close();
    }

    /**
     * Adds a specific process to the cgroups, giving that process the cgroup rules for that process.
     * If the Process ID list is empty, the PID of the current process gets passed on. In case of the
     * Process ID array not being empty, it checks for every process that is currently running, and
     * passes onto the cgroup control.
     *
     * @param processIds A list of process IDs to pass to the cgroup control
     * @throws IOException On cgroup or internal write errors
     */
    public void addProcess(long... processIds) throws IOException {
        if (processIds == null || processIds.length == 0) {
            addProcess();
            return;
        }

        File procsFile = new File(cgPath + "/tasks/cgroup.procs");
        if (!procsFile.exists())
            throw new IOException("cgroups failed to initialize");

        if (!procsFile.canWrite())
            throw new PermissionDeniedException("cannot write to a protected file as normal user");

        List<Long> pidList = new ArrayList<>();
        for (long procId : processIds) {
            Optional<ProcessHandle> pHandle = ProcessHandle.of(procId);
            pHandle.ifPresent(processHandle -> pidList.add(processHandle.pid()));
        }

        FileOutputStream fos = new FileOutputStream(procsFile);

        for (int i = 0; i < pidList.size(); i++) {
            fos.write(String.valueOf(pidList.get(i)).getBytes());

            if (i != pidList.size() - 1)
                fos.write("\n".getBytes());
        }

        fos.close();
    }

    public void setValue(String cgLabel, String cgValue) throws IOException {
        CGUtils.writeFile(new File(cgPath + "/" + cgLabel), cgValue);
    }

    public void setTaskValue(String cgLabel, String cgValue) throws IOException {
        CGUtils.writeFile(new File(cgPath + "/tasks/" + cgLabel), cgValue);
    }

    public String getValue(String cgLabel) throws IOException {
        return CGUtils.readFile(new File(findPath(cgLabel, false)));
    }

    public String getTaskValue(String cgLabel) throws IOException {
        String path = findPath(cgLabel, true);

        File file = new File(path);
        if (!file.exists())
            throw new FileNotFoundException("Cannot create a new file under cgroups v2");

        if (!file.canRead())
            throw new PermissionDeniedException("Cannot read values from tasks \"" + file.getName() + "\"");

        FileInputStream fis = new FileInputStream(file);
        StringBuilder str = new StringBuilder();
        byte[] buff = new byte[2048];
        int len;

        while ((len = fis.read(buff, 0, 2048)) != -1)
            str.append(new String(buff, 0, len));

        fis.close();

        return str.toString();
    }

    public static ControlGroup2 create(String cgName, String... cgControlTypes) throws IOException {
        Path cgRootPath = CGUtils.findCgMountPath();
        if (cgRootPath == null)
            throw new IOException("Cgroup mount path not found. Get cgroups mounted first.");

        File file = new File(cgRootPath + "/" + cgName);
        if (file.exists())
            return load(cgName);

        if (!file.mkdirs())
            throw new PermissionDeniedException("Could not create new cgroup control (Not root or superuser)");

        if (cgControlTypes != null) {
            File controls = new File(cgRootPath + "/" + cgName + "/cgroup.subtree_control");
            FileOutputStream fos = new FileOutputStream(controls, true);

            for (String control : cgControlTypes)
                fos.write(("\n+" + control).getBytes());

            fos.close();
        }

        Path controlPath = new File(cgRootPath + "/" + cgName).toPath();
        return new ControlGroup2(cgName, controlPath, cgControlTypes);
    }

    public static ControlGroup2 load(String cgName) throws IOException {
        Path cgRootPath = CGUtils.findCgMountPath();
        if (cgRootPath == null)
            throw new IOException("Cgroup mount path not found. Get cgroups mounted first.");

        File cgPath = new File(cgRootPath + "/" + cgName);
        if (!cgPath.exists())
            throw new FileNotFoundException("Control Group labeled \"" + cgName + "\" under \"" + cgRootPath + "\" not found");

        File file = new File(cgRootPath + "/" + cgName + "/cgroup.subtree_control");
        if (!file.exists())
            throw new FileNotFoundException("cgroup control directory or the cgroup subtree control file not found");

        FileInputStream fis = new FileInputStream(file);
        byte[] buff = fis.readAllBytes();
        fis.close();

        String[] types = new String(buff).split("\n");

        return new ControlGroup2(cgName, cgPath.toPath(), types);
    }

}

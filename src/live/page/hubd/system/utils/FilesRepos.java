package live.page.hubd.system.utils;

import live.page.hubd.system.Settings;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FilesRepos {

    public static File getFile(String src) {
        return new File(Settings.REPO + src);
    }

    /**
     * List Files recursively by extension in a specific folder
     *
     * @param directory  where search recursively
     * @param extensions to search
     * @return a List of File
     */
    public static List<File> listFiles(String directory, String... extensions) {
        List<String> extensionsList = Arrays.asList(extensions);
        List<File> files = new ArrayList<>();

        File[] files_repos = new File(Settings.REPO + "/" + directory).listFiles();
        if (files_repos != null) {
            for (File file : files_repos) {
                if (file.isDirectory()) {
                    files.addAll(listFilesSub(file.getAbsolutePath(), extensions));
                } else if (extensionsList.contains(file.getName().replaceFirst(".*\\.(" + StringUtils.join(extensionsList, "|") + ")$", "$1"))) {
                    files.add(file);
                }
            }
        }
        files.sort(Comparator.comparing(str -> {
            try {
                return str.getCanonicalPath();
            } catch (IOException ignore) {
            }
            return null;
        }));

        return files;
    }

    public static List<File> listFilesSub(String directory, String... extensions) {
        List<String> extensionsList = Arrays.asList(extensions);
        List<File> files = new ArrayList<>();

        File[] files_repos = new File(directory).listFiles();
        if (files_repos != null) {
            for (File file : files_repos) {
                if (file.isDirectory()) {
                    files.addAll(listFilesSub(file.getAbsolutePath(), extensions));
                } else if (extensionsList.contains(file.getName().replaceFirst(".*\\.(" + StringUtils.join(extensionsList, "|") + ")$", "$1"))) {
                    files.add(file);
                }
            }
        }

        return files;
    }


    public static List<File> listResourcesFiles(String extension) {
        List<File> files = new ArrayList<>();
        files.addAll(FilesRepos.listFiles("html", extension));
        files.sort(Comparator.comparing(File::getName));
        return files;
    }

}

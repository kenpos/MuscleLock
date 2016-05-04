package com.example.wekatest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.io.FileUtils;

public class Utility {

    public static void main(String[] args) throws IOException {
        File resourceAsFile = getResourceAsFile("file.name");
        System.out.println(resourceAsFile);
    }

    private static File getResourceAsFile(String resource) throws IOException {
        ClassLoader cl = Utility.class.getClassLoader();
        File file = null;
        FileResource fileResource = new URLClassLoaderFileResource(cl, resource);
        try {
            file = fileResource.getFile();
        } catch (IOException e) {
            fileResource = new ClasspathResourceFileResource(cl, resource);
            file = fileResource.getFile();
        }
        return file;
    }

    public static interface FileResource {

        public File getFile() throws IOException;

    }

    public static class ClasspathResourceFileResource implements FileResource {

        private ClassLoader cl;
        private String resource;

        public ClasspathResourceFileResource(ClassLoader cl, String resource) {
            this.cl = cl;
            this.resource = resource;
        }

        public File getFile() throws IOException {
            InputStream cpResource = cl.getResourceAsStream(resource);
            File tmpFile = File.createTempFile("file", "temp");
            FileUtils.copyInputStreamToFile(cpResource, tmpFile);
            tmpFile.deleteOnExit();
            return tmpFile;
        }

    }

    public static class URLClassLoaderFileResource implements FileResource {

        private ClassLoader cl;
        private String resource;

        public URLClassLoaderFileResource(ClassLoader cl, String resourcePath) {
            this.cl = cl;
            this.resource = resourcePath;
        }

        public File getFile() throws IOException {
            File resourceFile = null;
            if (cl instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = URLClassLoader.class.cast(cl);
                URL resourceUrl = urlClassLoader.findResource(resource);
                if ("file".equals(resourceUrl.getProtocol())) {
                    try {

                        URI uri = resourceUrl.toURI();
                        resourceFile = new File(uri);
                    } catch (URISyntaxException e) {
                        IOException ioException = new IOException(
                                "Unable to get file through class loader: "
                                        + cl);
                        ioException.initCause(e);
                        throw ioException;
                    }

                }
            }
            if (resourceFile == null) {
                throw new IOException(
                        "Unable to get file through class loader: " + cl);
            }
            return resourceFile;
        }

    }
}
The file `jvm.config` has a necessary value, `--enable-preview` that is used during 
compilation and running in Maven targets.  This is required because we are using virtual
threads, which is a preview feature in Java 20.
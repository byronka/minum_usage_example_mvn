Minum example project
=====================

This is a simple project meant to demonstrate use of the [Minum framework](https://github.com/byronka/minum).
A good starting point for reading this code is the [Main](src/main/java/com/renomad/Main.java) and
[TheRegister](src/main/java/com/renomad/TheRegister.java) class. 


Quick start:
------------

* To run: 

```shell
./mvnw compile exec:java
```
then visit http://localhost:8080

Press ctrl+c to stop.

* To test:

```shell
./mvnw test
```


System requirements: 
--------------------

[JDK version 21](https://jdk.java.net/21/) is _required_, since it
provides us the [virtual threads](https://openjdk.org/jeps/444) we need.

Developed in two environments:
* MacBook Pro with OS 12.0.1, with OpenJDK 21
* Windows 10 64-bit professional, on [Cygwin](https://www.cygwin.com/), OpenJDK 21



Step-by-step guide for installing Java on Windows:
--------------------------------------------------

1. Download the binary by clicking [here](https://jdk.java.net/21/) and selecting the Windows/x64 zip.
2. Uncompress the zip file
3. Add the home directory to your path.  The home directory of Java is the one with "bin"
   and "conf" directories, among others. if you, for example, uncompressed the
   directory to C:\java\jdk-21, then in Windows you should add it to your path,
   following these instructions:

* Click the Windows start icon
* Type `env` to get the system properties window
* Click on _Environment Variables_
* Under user variables, click the _New_ button
* For the variable name, enter `JAVA_HOME`, and for the value, enter `C:\java\jdk-21`
* Edit your _Path_ variable, click _New_, and add `%JAVA_HOME%\bin`


First-time use
--------------

1. Start the application (see [above](#quick-start))
2. Go to http://localhost:8080
3. Soak up the austere beauty of the homepage
4. Click on **Auth**
5. Click on **Register**
6. Enter some credentials, such as _foo_ and _bar_, click Enter
7. Enter them again (you're logging in this time)
8. Click on **Sample domain**
9. Click on **Enter a name**
10. Enter some names, pressing **Enter** each time.
11. Click **Index**
12. Click on **Photos**
13. Click on **Upload**
14. Select an image, enter a short caption and longer description 
15. Click **Submit**
16. Click **Index**
17. Click on **Auth**
18. Click on **Logout**
19. Go view the other pages again

Directories:
------------

- src: All the source code, including production and test code
- .git: necessary files for Git.

Root-level files:
-----------------

- minum.config: a configuration file for the running app (a local / test-oriented version)
- .gitignore: files we want Git to ignore.
- LICENSE: the license to this software
- pom.xml: Maven's configuration file
- README.md: this file

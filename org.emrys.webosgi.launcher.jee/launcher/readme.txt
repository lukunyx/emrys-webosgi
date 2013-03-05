This JavaEE application is the bridge launcher for OSGi Web Container.
* Copy your .war archive file or a folder to the dir WEB-INF/framework/webapps/, this war or folder should be a WAB bundle.
*     What a WAB? Refer to Chapter.128(Web Application/Web Container) of OSGi Service Platform Enterprise Specification Release 4, Version 4.2.
* A WAB can be developped as as a standard JavaEE war project, but it can benefit more form OSGi framework.

This launcher application itself is a WAB, and you can place your JavaEE resources in this directory and do modification to web0.xml
Note:
  1. web.xml is the bridge Servelet Configuration file which use by framework. Do not modify, remove or rename it.
  2. web0.xml is actually the customer JavaEE configuration file which you can do any according to the JavaEE 2.5 standard.
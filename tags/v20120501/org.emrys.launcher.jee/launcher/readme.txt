This directory contains a JavaEE launcher's structure of the 
Framework which be wrapped in a JavaEE application. You can 
export all you plugins to sub directory WEB-INF/framework/plugins 
and copy this directory wholly to a JavaEE Server's deploy root, 
and start the server.

1. web.xml is the bridge Servelet Configuration file which use by framework. Do not modify, remove or rename.
2. web0.xml is actually the customer JavaEE configuration file which you can do any according to the JavaEE standard.
# emrys-webosgi
Automatically exported from code.google.com/p/emrys-webosgi
# WebOSGi介绍
WebOSGi是一种将OSGi插件化架构应用与JavaEE应用的解决方案，由Emrys开源软件社区负责开发维护。该方案基于OSGi的Equinox开源项目，以OSGi 插件的方式，实现了一个轻量级的JavaEE Web Container框架，使得以OSGi插件化方式开发大型JavaEE应用变为可能。该框架完全遵循OSGi Enterprise Specification r4.2规范文档中第128章关于Web Container的定义，同时又提供了若干实用、方便的个性化、扩展接口。

# WebOSGi方案给开发者带来如下好处：
将现有的JavaEE应用，不加修改的迁移和部署到WebOSGi环境中 该方式仅仅类似将JavaEE应用包部署到一个底层采用OSGi架构的JavaEE服务器上，而且没有从OSGi得到任何的好处，所以不建议采用此方式。
对现有JavaEE应用，以OSGi插件开发方式，进行稍微的插件拆分、改造，然后部署到WebOSGi中 如果开发者原有的JavaEE应用，虽然采用了Spring等流行的中间件，功能代码耦合比较严重，协同开发、部署、产品升级、jars版本依赖管理难度大，那么我们推荐使用WebOSGi架构，将该项目划分为若干个松耦合的OSGi插件，进行相应的架构设计。
开发新项目时，就采用OSGi插件化的设计思想，将大型的、需求繁杂多变的JavaEE项目，按照功能更耦合依赖关系，划分为若干个不同层次的OSGi 插件项目进行开发，最终部署到WebOSGi环境中。
使用JSF, JPA, Spring, Struts, Hibernate等常用的JavaEE中间件的OSGi插件版进行开发，便于这些中间件的共享。
使用OSGi这个优秀的java插件化架构提供所有优良特性，比如：
允许将一个大型的JavaEE项目，原来是打包为一个很大的war文件，对功能耦合依赖进行抽象归纳，合理的划分为若干个具有依赖层级关系的jar。这种简单的、单向的、稳定的模块依赖关系，使得团队协作、扩展开发、更新升级更加的高效。
允许JavaEE中的API通过OSGi 服务器总线发布和获取，甚至扩展为企业服务总线ESB
在开发期、构建起、运行期使用OSGi管理各个插件项目的版本依赖关系和范围。
OSGi使得JavaEE应用插件jars拥有完全的生命周期，我们在其启动、停止等阶段，可以方便实现我们想要的逻辑。相比以前，项目中大量的jar，每个jar都被一起加入loader,这是一个非常有用的特性。
有了OSGi的插件依赖管理，可以方面的使用插件的热插拔技术，甚至实现产品在线更新。当然，此特性对于服务器端插件来说，除非是大量分发的产品，可用性不如客户端应用强。
如果是基于Equinox OSGi实现（WebOSGi框架就是)，可以利用Equinox提供的扩展点机制，在JavaEE应用中方便的定义和使用扩展点，实现各种高度可扩展的插件化功能。这种扩展，不局限于后端服务的扩展，也可以用户前段界面的插件化扩展。典型的应用场景首页或者工作台上的导航菜单，核心用户只提供首页界面框架，有其他插件应用通过扩展点向其导航菜单上扩展不同的界面菜单。
WebOSGi方案具有以下特征：
遵循OSGi Enterprise R4.2 Specification规范文档中第128章关于Web Container的定义。使得JavaEE插件项目可以平滑的迁移到其他平台之上。
WebOSGi Web Container遵循Servlet2.5规范
WebOSGi框架采用外部标准JavaEE Server和桥接Servlet的方式启动部署(Equinox官方推荐两种方案之一)，使得WebOSGi可以部署与当前众多标准的JavaEE Server上(tomcat5+, JBoss, WebLogic等)。该方式相比另一种OSGi内嵌Web服务器（例如Jetty，tomcat-embed等），优点如下：
充分的利用现有服务器的高并发和高效IO能力，OSGi内嵌服务器，在大并发请求压力下，响应并不尽如人意。
便于WebOSGi JavaEE项目与普通的项目统一集中服务器部署。
同时沿用了之前的JavaEE部署方式，降低了原有JavaEE开发者学习成本。
框架不依赖OSGi嵌入式服务器若干插件，使得框架更加轻量级。
插件化JavaEE应用部署方式不仅支持规范定义.war文件和OSGi插件jar两种方式，而且支持文件夹方式。这样为从IDE中直接调试提供了支持。（我们可以将项目代码根目录放于WebOSGi 部署目录中)
WebOSGi支持JPA1.1, JSF, Spring, Hibernate, Struts等常用JavaEE中间件，并且提供了相应的OSGi封装插件（也可下载其官方的OSGi插件版本)，使得开发者方便的依赖这些中间件插件，按照原有习惯进行开发，而不用担心其大量的jars版本管理、冲突等问题。同时，WebOSGi运行环境中，甚至可以允许Spring等中间件的多个版本同时存在，被多个项目共享。如果不以OSGi插件方式封装这些中间件，也可以将这些jars打包在Web App内部，作为插件的内部库使用。
JavaEE开发者，不需要深入了解OSGi相关技术，就可以以插件化方式开发JavaEE应用。
WebOSGi项目开发计划
已经完成：
2012.5.1 发布 1.0版本 实现了核心Web Container机制，支持使用插件方式部署JavaEE。
2012.12.1 发布1.1版本 全部兼容OSGi Enterprise r4.2 Specification 第128章中关于Web Container的定义。
正在开发：
2013.6.1 发布1.2版本
修正已知bugs，优化性能，改进启动配置（Host,Port）
支持动态感知war等安装更新。
未来规划：
2013.12.1 发布1.4版本
增加Web Console，提供在线更新、安装Wab应用和管理服务器
增加基本的管理、配置界面

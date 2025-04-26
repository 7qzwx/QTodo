# QDemo：Compose模版

> 用于快速创建jetpackcompose项目

## 已引入的第三方库
- ksp,room，hilt，lottie，约束布局，导航，图标拓展库模块；

## 已添加代码
- 增加QZWXApplication模块，用于全局配置；
- 增加底部导航组件，已与NavHost搭配；
- 增加字体，修改了字体类型

## 需要修改地方
- kotlin版本为2.1.20
- 在Setting.gradle.kts中修改 ``` rootProject.name = "QDemo" ```
- 在Build.gradle.kts（app模块）中修改命名空间等名称  ```applicationVariants.all {XXX}```
  这里配置的是打包apk的名称
- 在Manifest中修改主题名称等
- 在修改目录中文件夹名称

# 2025年4月27日1.0版本已经完成。

- 1.0版本已经完成。



<video src="ScreenVideo/1.0_QTodo.mp4"></video>

## 如何从其他应用启动QTodo

其他应用可以通过以下两种方式启动QTodo应用：

### 方式一：使用自定义动作

```java
Intent intent = new Intent("qzwx.app.qtodo.LAUNCH");
startActivity(intent);
```

### 方式二：使用包名直接启动

```java
Intent intent = new Intent();
intent.setComponent(new ComponentName("qzwx.app.qtodo", "qzwx.app.qtodo.MainActivity"));
startActivity(intent);
```

### 传递参数

如需在启动时传递参数，可在Intent中添加额外数据：

```java
Intent intent = new Intent("qzwx.app.qtodo.LAUNCH");
intent.putExtra("key_name", "value");
startActivity(intent);
```

然后在MainActivity中获取这些参数：

```java
String value = getIntent().getStringExtra("key_name");
```

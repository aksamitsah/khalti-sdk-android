# khalti-android
SDK for Khalti Android App

## Installation
Add the following line to `dependency` section in `build.gradle` file

```
compile 'com.khalti:khalti-android:1.0.8'
```
It is recommended that you update your support libraries to the latest version. However, if you're unable to update the libraries add the following line instead.

```
compile ('com.khalti:khalti-android:1.0.8') {
        transitive = true
    }
```
Note : We recommend you to use the latest version of `Build tools` and `Support libraries` for maximum compatibility. 

In order to build and run this project, please use `Android Studio 3` and please note that the minimum `Build tools` and `Support libraries` version should be `26`.

```
compileSdkVersion 26
buildToolsVersion '26.0.2'

compile 'com.android.support:appcompat-v7:26.1.0'
```
In order to add support library 26, add the Google's maven url in `build.gradle`

```
repositories {
        jcenter()
        mavenCentral()
        maven { url "https://maven.google.com" }
    }
```
## Usage

#### Layout

You can add it to your xml layout
```xml
<khalti.widget.KhaltiButton
            android:id="@+id/khalti_button"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"/>


```
Or, use it in Java

``` java
KhaltiButton khaltiButton = new KhaltiButton();
```
#### Configure

Configure Khalti Checkout by passing an instance of Config class

When instantiating Config class pass public key, product id, product name, product web url, amount (in paisa) and a new instance of OnCheckOutListener.
```java
Config config = new Config("Public Key", "Product ID", "Product Name", "Product Url", amount, new OnCheckOutListener() {

            @Override
            public void onSuccess(HashMap<String, Object> data) {
                Log.i("Payment confirmed", data+"");
            }

            @Override
            public void onError(String action, String message) {
                Log.i(action, message);
            }
        });
```
Additionally, Config class also accepts a HashMap parameter which you can use to pass any additional data. Make sure you add a `merchant_` prefix in your map key.

``` java
HashMap<String, Object> map = new HashMap<>();
        map.put("merchant_extra", "This is extra data");
        
        Config config = new Config("Public Key", "Product ID", "Product Name", "Product Url", amount, map, new OnCheckOutListener() {

            @Override
            public void onSuccess(HashMap<String, Object> data) {
                Log.i("Payment confirmed", data);
            }

            @Override
            public void onError(String action, String message) {
                Log.i(action, message);
            }
        });

```
#### Set Config
Finally set your config in your KhaltiButton.

``` java
khaltiButton.setCheckOutConfig(config);
```

Check out the [documentation](http://docs.khalti.com/checkout/android/) for further details.

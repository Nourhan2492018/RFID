# RF SDK接入文档

|  版本号 | 更新说明|更新人|
|---|---|---|
|  1.0.0 |  1.0.0发布 | yefl |

------



### 一、接入SDK
#### 1.将lib_rfid_vxxx.aar放在libs文件夹下

#### 2.添加依赖

```
ndk {
    abiFilters 'armeabi-v7a'
}

```
#### 3.依赖库
```
implementation 'com.blankj:utilcode:1.25.7'
implementation 'io.reactivex.rxjava2:rxandroid:2.1.1'
```

#### 4.权限

```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />//RF2需要
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />//RF2需要
<uses-permission android:name="android.permission.BLUETOOTH" />  //RF2需要
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />//RF2需要
```

#### 5.初始化

```
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RFHelper.init(this)
    }
}
```

### 二、常量说明

#### 错误状态说明：

0x0000:操作成功

0X0100:数据实际长度跟长度字节的值不一样

0X0101:不可用命令

0X0105:不可用参数值

0X010A:不可用波特率

0X010B:不可用区域选择（北美，中国，欧洲等）

0X0200:应用层程序 CRC 不正确，校验应用层程序 CRC 错误。

0X0302:FLASH 未定义错误，FLASH 写入失败。

0X0400:未找到标签（盘存读写 LOCK 等 操作失败，没找到标签）

0X0402:协议不可用（GEN2 或 6B 什么的）

0X040A:一般标签错误（读写 LOCK,KILL 命令）

0X040B:读内存的长度值超限。（如只能读 96 个字一次，

0X040C:不可用的 KILL 密码

0X0420:GEN2 OTHER ERR

0X0423:MEMORY OVERRUN BAD PC

0X0424:MEM LOCKED

0X042B:INSUFFICIENT POWER

0X042F:NON SPECIFIC ERR

0X0430:UNKNOWN ERR

0X0500:不可用频率值

0X0504:温度超限

0X0505:反射过大

0X7F00:系统不知道的错误，严重错误。

0XAA2A:OEM 格式化失败

0XAA02:OEM 写失败

0XAA03:OEM 读失败

0XAA04:测试校验命令失败

0XAA1B:GROSS GAIN 校验失败

0XAA24:命令失败

0XAA27:命令失败

0XAA2C:命令失败

0XAA2E:MAC 寄存器写失败

0XAA2F:MAC 寄存器读失败

0XFF01:初始化定时器出错；

0XFF02:OEM 初始化失败；

0XFF03: 失败

0XFF04: 失败

0XFF05: 失败

0XFF06: 失败

0XFF07: 失败

0XFF08: 失败

0XFF09: GPIO 配置出错

0XFF0A: QM100 芯片初始化失败

0XFF0B: 失败

0XFF0C: 失败

0XFF0D: 失败

0XFF0E: 失败


### 三、方法

#### 1.连接设备
##### 串口连接

````
connect(path: String, baudrate: Int, listener:ConnectListener)
````
参数:
- **path**:串口地址="/dev/ttyS1"
- **baudrate**:波特率=115200

```
interface ConnectListener {
       fun onSuccess() //连接成功
       fun onFail(e:Exception) //连接失败
}
```

##### 蓝牙连接（RF2使用）
````
connect(mac: String, listener:ConnectListener)
````
参数：
- **mac**：蓝牙mac地址

```
interface ConnectListener {
       fun onSuccess() //连接成功
       fun onFail(e:Exception) //连接失败
}
```

#### 2.断开连接

````
 RFHelper.disconnect()
````

#### 3.获取设备版本
````
getControlVersion(listener:VersionListener)
````
参数:
- VersionListener:接口回调，回调线程为子线程

```
public interface VersionListener {
    void getVersion(String version);
}
```

#### 4.获取设备SN
````
getControlSN(listener:SNListener)
````
参数：
- **SNListener**：接口回调，回调线程为子线程

```
public interface SNListener {
    void getSn(String sn);
}
```
#### 5.获取电量
````
getControlBattery(listener:BatteryListener)
````
参数：

- BatteryListener：接口回调，回调线程为子线程

````
public interface BatteryListener {
    void getBattery(byte battery);
}
````

#### 6.控制蜂鸣器和手柄闪灯

- 盘点和其他操作有回复时调用此方法，操作手柄蜂鸣器发声和闪灯

````
controlTwinkle()
````

#### 7.关机

- 手柄关机方法

````
poweroff()
````

#### 8.设置蜂鸣器音量

````
controlBuzzer(sound: Int)
````

参数：

- **sound**：音量值， 取值范围【0-100】

#### 9.单标签盘存

````
singleInventory(
        timeout: Int,
        option: ByteArray,
        metadataflags: ByteArray,
        tagSingulationFields: ByteArray
    )
````

参考RFID模块文档. 0x21:单标签盘存命令

该命令为在指定的时间内盘存一个标签 EPC 号，也即是在指定时间内盘存到一个标签后就返回，盘存时间由两个字节组成，单位为 毫秒

参数：

- **timeout**：超时时间
- **option**：盘存select options
- **metadataflags**: 盘存Metadata Flags
- **tagSingulationFields**:由Select Options的值决定，如果启用选择匹配过滤，则相关的数据接在Access Password数据后面

例子：

````
 binder?.singleInventory(
     5000,
     byteArrayOf(0x10),
     byteArrayOf(0x00, 0xFF.toByte()),
     byteArrayOf(),
     mReadTagSingleListener
 )
````



#### 10.多标签盘存

````
multiInventory() 
````

参考RFID模块文档.0x22:多标签盘存命令

步骤为：先发送0x22盘存命令，如果盘存到标签，在发送0x29命令取盘存到的标签信息

#### 11.获取RFID数据信息

````
getRFIDData(listener:RfidDataListener)
````

参数：

- **RfidDataListener**：数据接口

````
public interface RfidDataListener {
    void onRfid(RFIDEntity data);
}
````

根据返回的RFIDEntity中command来区分不同命令的回复。详见demo RFService中类别

|  命令值 | 命令内容 |
|---|---|
|  0x21 |  单标签盘存命令 |
|  0x22 |  多标签盘存命令 |
|  0x23 |  写标签EPC命令 |
|  0x24 |  写标签存储器命令 |
|  0x25 |  LOCK标签命令 |
|  0x26 |  KILL标签命令 |
|  0x28 |  读标签存储区命令 |
|  0x29 |  获取盘存到标签信息命令 |
|  0x2a |  清除标签缓冲区命令 |

#### 12.获取功率
````
    getAntPower()
````
获取读写功率
````
    RFHelper.getAntPower()
````
例子：
````
var mSettingListener = object :SettingListener{
    override fun onAntPower(rfidEntity: RFIDEntity){
        val curReadPower = ByteUtils.bytes2ToInt_h(rfidEntity.data, 2)
        val curWritePower = ByteUtils.bytes2ToInt_h(rfidEntity.data, 4)
    }
}
````

#### 13.获取读取功率
````
getReadPower()
````
#### 14.获取写入功率
````
getWritePower()
````
#### 15.设置功率
````
setPower(readPower: Int, writePower: Int)
````
#### 16.获取取工作频率
````
getFrequency()
````
#### 17.设置工作频率
````
setFrequency(code: ByteArray)
````
例子：
````
when(sp_freq.selectedIndex){
    0->code=0x06
    1->code=0x0a
    2->code=0x01
    3->code=0x08
}
binder?.setFrequency(byteArrayOf(code.toByte()), mFreqSettingListener)
````
|区域|设置值code|
|---|---|
|北美(902-928)|0x01|
|中国1(920-925)|0x06|
|欧频(865-867)|0x08|
|中国2(840-845)|0x0a|

#### 18.获取跳频表
````
getHoppingFreqency()
````
#### 19.设置跳频
````
setHoppingFrequency(code: ByteArray)
````

#### 20.读取标签存储区
`````
readStorageArea(
        timeout: Int,
        option: Int,
        metadataFlags: ByteArray,
        readMemBank: Int,
        readAddr: ByteArray,
        wordCount: Int,
        password: ByteArray,
        tagSingulationFields: ByteArray
        )
`````
参数：

- **timeout**：读取超时时间

- **option**：盘存select options

- **metadataflags**: 盘存Metadata Flags

- **readMemBank**:指定存储区： 0x00=Reserved 0x01=EPC 0x02=TID 0x03=User Memory

- **readAddr**：读起始字地址，存储区的字地址(16BITS),地址是从0开始的，地址为0表示从首地址开始

- **wordCount**：读取的字数(16BITS)， 一次最多只能读96个字

- **password**：访问密码，如果存储区未锁读，则password=0x00000000，锁读则需要正确密码。备注：option=0时，访问密码不需要包括在命令中

- **tagSingulationFields**:由Select Options的值决定，如果启用选择匹配过滤，则相关的数据接在Access Password数据后面

#### 21.获取盘存到的标签信息

````
getTagInfo()
````

#### 22.写标签存储区

````
writeTagData(
        timeout: Int,
        option: Int,
        writeAddr: ByteArray,
        writeMemBank: ByteArray,
        writeData: ByteArray,
        password: ByteArray,
        tagSingulationFields: ByteArray
    )
````

把数据写入指定的标签存储区中指定的地址。第一个被盘存到的标签将会被写入。

参数：

- **timeout**：读取超时时间
- **option**：盘存select options
- **writeAddr**: 4bytes, 写起始地址，为指定写入的存储区的写入起始地址，该地址为字地址（16BITS）。地址是从0开始的，地址为0表示从首地址开始。
- **writeMemBank**：写入存储区：
  0x00=Reserved 
  0x01=EPC 
  0x02=TID 
  0x03=UserMemory
- **writeData**：N bytes, 写入数据。写入数据字节长度必须为2的倍数。一次最多只能写32个字，即64个字节
- **password**：4bytes, 访问密码。如果写入的存储区未锁定，则密码AccessPwd=0x00000000即可，如果写入的存储区锁定，则密码必须为正确的密码才行。
  备注：当Option=0x00时，命令串中不包括访问密码。
- **tagSingulationFields**:由Select Options的值决定，如果启用选择匹配过滤，则相关的数据接在Access Password数据后面

#### 23.写标签EPC

````
writeTagEPCData(
        timeout: Int,
        option: Int,
        password: ByteArray,
        tagSingulationFields: ByteArray,
        epcid: ByteArray
    )
````

参数：

- **timeout**：2bytes， 写处理时间，单位毫秒
- **option**：1byte, Select Options
- **password**：4bytes，访问密码，如果标签EPC域未锁定，则为0X00000000；如果EPC域锁定，则只有访问密码正确时才能够写入。
  注意：如果SelectOptions=0，命令中不包含该4字节访问密码。
- **tagSingulationFields**:由SelectOptions的值决定，如果启用选择匹配过滤，则相关的数据接在password数据后面
- **epcid**:N bytes， 最多496位标签EPC ID(取决于标签)

#### 24.LOCK标签

````
lockTag(
        timeout: Int,
        option: Int,
        password: ByteArray,
        maskbits: ByteArray,
        actionBits: ByteArray,
        tagSingulationFields: ByteArray
    )
````

锁定或者解锁指定的存储区，Option=0x05 不能用在该命令

参数：

- **timeout**：2bytes， 写处理时间，单位毫秒

- **option**：1byte, Select Options

- **password**：4bytes，访问密码，如果标签EPC域未锁定，则为0X00000000；如果EPC域锁定，则只有访问密码正确时才能够写入。
  注意：如果SelectOptions=0，
  命令中不包含该4字节访问密码。

- **maskbits**：2bytes， 见RFID 0x25表，对应的位为 1 时表示执行对应的ACTIONBIT位的操作

- **actionBits**：2bytes， ACTIONBIT 中的位只有当对应的MASKBIT位为1时才起作用。0 为解锁，1 为锁定

- **tagSingulationFields**:由SelectOptions的值决定，如果启用选择匹配过滤，则相关的数据接在password数据后面

  

#### 25.KILL标签

````
KillTag(
        timeout: Int,
        option: Int,
        password: ByteArray,
        rfu: ByteArray,
        tagSingulationFields: ByteArray
    ) 
````

销毁标签指令， **Option=0x05** 不能用在该命令

参数：

- **timeout**：2bytes，操作超时时间， 单位毫秒

- **option**：1byte, Select Options

- **password**：4bytes，访问密码，如果标签EPC域未锁定，则为0X00000000；如果EPC域锁定，则只有访问密码正确时才能够写入。
  注意：如果SelectOptions=0，命令中不包含该4字节访问密码。
  
- **rfu**：1bytes， RFU 为一个字节，预留用，为 0x00

- **tagSingulationFields**:由SelectOptions的值决定，如果启用选择匹配过滤，则相关的数据接在password数据后面

#### 26.快速模式（RF1不支持， RF1P RF2支持）

````
fastInventory()
````

#### 27.快速模式（设置占空比， RF1不支持， RF1P RF2支持）

````
fastInventory(speed:Int)
````

参数：

- **speed**：目前支持100， 50， 20， 分别表示全速， 50%速度， 20%速度

#### 28.停止快速模式（RF1不支持， RF1P RF2支持）

````
stopFastInventory()
````

#### 29.检查手柄是否连接(RF1 RF1P支持)
````
 while (true){
    val result = FileIOUtils.readFile2String("/sys/devices/platform/10010000.kp/TSTBASE")
    if(result!= null && (result.equals("1") || result.equals("1\n"))){
        //手柄已连接
    }else{
        ThreadExecutors.mainThread.execute {
            EventBus.getDefault().post("lost","handleLost")
            ToastUtils.showShort("手柄断开")
        }
        binder?.stopFastInventory()
        binder.disconnect()
        break
    }
    sleep(5*1000)
}
````


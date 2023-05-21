# RF SDK access document

|  Version Number | Update Description|Update Person|
|---|---|---|
|  1.0.0 |  1.0.0 | yefl |

------



### One、Access SDK
#### 1.Put lib_rfid_vxxx.aar under Folder "libs" 

#### 2.Add dependency

```
ndk {
    abiFilters 'armeabi-v7a'
}

```
#### 3.Dependency library
```
implementation 'com.blankj:utilcode:1.25.7'
implementation 'io.reactivex.rxjava2:rxandroid:2.1.1'
```

#### 4.Permission

```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />//RF2需要
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />//RF2需要
<uses-permission android:name="android.permission.BLUETOOTH" />  //RF2需要
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />//RF2需要
```

#### 5.Initialization

```
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RFHelper.init(this)
    }
}
```

### Two、initialization

#### Error status description：

0x0000:Operation is successful

0X0100:The actual length of the data is different from the value of the length byte

0X0101:Unavailable commands

0X0105:Unavailable parameter value

0X010A:Unavailable baud rate

0X010B:Unavailable region selection (North America, China, Europe, etc.)

0X0200:Application layer program CRC is incorrect，Verify application layer program CRC is wrong.

0X0302:FLASH Undefined error，FLASH Write failed.

0X0400:Label is not found（Inventory reading and writing LOCK and so on, operation failed，label is not found）

0X0402:Protocol is not available（GEN2 or 6B ）

0X040A:General label error（Reading and writing LOCK,KILL command）

0X040B:The length value of read memory exceeds the limit.（For example it can only read 96 words once，

0X040C:Unavailable KILL password

0X0420:GEN2 OTHER ERR

0X0423:MEMORY OVERRUN BAD PC

0X0424:MEM LOCKED

0X042B:INSUFFICIENT POWER

0X042F:NON SPECIFIC ERR

0X0430:UNKNOWN ERR

0X0500:Unavailable frequency value

0X0504:Temperature overrun

0X0505:Excessive reflection

0X7F00:Error unknown to the system, serious error

0XAA2A:OEM format failure

0XAA02:OEM Write failed

0XAA03:OEM Read failed

0XAA04:Test verification command failed

0XAA1B:GROSS GAIN Verification failed

0XAA24:command failed

0XAA27:command failed

0XAA2C:command failed

0XAA2E:MAC Register write failed

0XAA2F:MAC Register read failed

0XFF01:Error initializing timer；

0XFF02:OEM Initialization failed；

0XFF03: failed

0XFF04: failed

0XFF05: failed

0XFF06: failed

0XFF07: failed

0XFF08: failed

0XFF09: GPIO Configuration error

0XFF0A: QM100 Chip initialization failed

0XFF0B: failed

0XFF0C: failed

0XFF0D: failed

0XFF0E: failed


### Three、Method

#### 1.Connecting equipment
##### serial port connection 

````
connect(path: String, baudrate: Int, listener:ConnectListener)
````
parameter:
- **path**:Serial port address="/dev/ttyS1"
- **baudrate**:Baud rate=115200

```
interface ConnectListener {
       fun onSuccess() //Connection is successful
       fun onFail(e:Exception) //connection is failed
}
```

##### Bluetooth connection（RF2 use）
````
connect(mac: String, listener:ConnectListener)
````
parameter：
- **mac**：bluetooth mac address

```
interface ConnectListener {
       fun onSuccess() //Connection is successful
       fun onFail(e:Exception) //connection is failed
}
```

#### 2.Disconnecting

````
 RFHelper.disconnect()
````

#### 3.Get device version
````
getControlVersion(listener:VersionListener)
````
parameter:
- VersionListener:Interface callback. The callback thread is a sub thread

```
public interface VersionListener {
    void getVersion(String version);
}
```

#### 4.Get device SN
````
getControlSN(listener:SNListener)
````
parameter：
- **SNListener**：Interface callback. The callback thread is a sub thread

```
public interface SNListener {
    void getSn(String sn);
}
```
#### 5.Get Battery
````
getControlBattery(listener:BatteryListener)
````
parameter：

- BatteryListener：Interface callback. The callback thread is a sub thread

````
public interface BatteryListener {
    void getBattery(byte battery);
}
````

#### 6.Control buzzer and handle flashing light

- Use this method when there is a reply to inventory and other operations, and the buzzer of the operating handle will sound and flash

````
controlTwinkle()
````

#### 7.Shut down

- Handle shutdown method

````
poweroff()
````

#### 8.Set buzzer volume

````
controlBuzzer(sound: Int)
````

parameter：

- **sound**：Volume value， Value range【0-100】

#### 9.Single label inventory

````
singleInventory(
        timeout: Int,
        option: ByteArray,
        metadataflags: ByteArray,
        tagSingulationFields: ByteArray
    )
````

Refer to RFID module documentation. 0x21:Single label inventory command

This command is to save a tag EPC number within the specified time, that is, it returns after saving a tag within the specified time. The inventory time consists of two bytes, with the unit of ms

parameter：

- **timeout**：Timeout
- **option**：take inventory select options
- **metadataflags**: take inventory Metadata Flags
- **tagSingulationFields**:It is determined by the value of Select Options. If select matching filtering is enabled, the relevant data will be followed by the Access Password data

example：

````
 binder?.singleInventory(
     5000,
     byteArrayOf(0x10),
     byteArrayOf(0x00, 0xFF.toByte()),
     byteArrayOf(),
     mReadTagSingleListener
 )
````



#### 10.Multi label inventory

````
multiInventory() 
````

Refer to RFID module documentation.0x22:Multi label inventory command

The steps are as follows: first send 0x22 inventory command，If the inventory is to the tag，then send 0x29command to get the label information of the inventory

#### 11.Get RFID data information

````
getRFIDData(listener:RfidDataListener)
````

parameter：

- **RfidDataListener**：data interface 

````
public interface RfidDataListener {
    void onRfid(RFIDEntity data);
}
````

The replies of different commands are distinguished according to the commands in the returned RFIDEntity.See category demo RFService for details

|  Command value | Command content |
|---|---|
|  0x21 |  Single label inventory command |
|  0x22 |  Multi label inventory command |
|  0x23 |  Write label EPC command |
|  0x24 |  Write label memory command |
|  0x25 |  LOCK label command |
|  0x26 |  KILL label command |
|  0x28 |  Read label store command |
|  0x29 |  Get inventory to label information command |
|  0x2a |  Clear label buffer command |

#### 12.Get power
````
    getAntPower()
````
Get read and write power
````
    RFHelper.getAntPower()
````
example：
````
var mSettingListener = object :SettingListener{
    override fun onAntPower(rfidEntity: RFIDEntity){
        val curReadPower = ByteUtils.bytes2ToInt_h(rfidEntity.data, 2)
        val curWritePower = ByteUtils.bytes2ToInt_h(rfidEntity.data, 4)
    }
}
````

#### 13.Get read power
````
getReadPower()
````
#### 14.Get write power
````
getWritePower()
````
#### 15.Set power
````
setPower(readPower: Int, writePower: Int)
````
#### 16.Get working frequency
````
getFrequency()
````
#### 17.Set operating frequency
````
setFrequency(code: ByteArray)
````
example：
````
when(sp_freq.selectedIndex){
    0->code=0x06
    1->code=0x0a
    2->code=0x01
    3->code=0x08
}
binder?.setFrequency(byteArrayOf(code.toByte()), mFreqSettingListener)
````
|region|Set value code|
|---|---|
|North America(902-928)|0x01|
|China1(920-925)|0x06|
|Europe(865-867)|0x08|
|China2(840-845)|0x0a|

#### 18.Get frequency hopping table
````
getHoppingFreqency()
````
#### 19.Set frequency hopping
````
setHoppingFrequency(code: ByteArray)
````

#### 20.Read label store area
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
parameter：

- **timeout**：Read timeout

- **option**：take inventory select options

- **metadataflags**: take inventory Metadata Flags

- **readMemBank**:Specify storage area： 0x00=Reserved 0x01=EPC 0x02=TID 0x03=User Memory

- **readAddr**：Read start word address，Word address of storage area(16BITS),the address starts at 0，An address of 0 means starting from the first address

- **wordCount**：Number of words read(16BITS)， it can only read 96 words at a time

- **password**：Access password，If the store is not locked for reading，password=0x00000000.The correct password is required for lock reading.When remarks：option=0，the access password does not need to be included in the command

- **tagSingulationFields**:Determined by the value of Select Options，If select match filtering is enabled, the relevant data is followed by the Access Password data

#### 21.Get the label information of the inventory

````
getTagInfo()
````

#### 22.Write label store area

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

Write data to the address specified in the specified label storage area.The first label to be inventoried will be written.

parameter：

- **timeout**：Read timeout
- **option**：take inventory select options
- **writeAddr**: 4bytes, Write start address is the write start address of the specified write storage area, which is the word address（16BITS）.The address starts at 0，An address of 0 means starting from the first address
- **writeMemBank**：Write to storage：
  0x00=Reserved 
  0x01=EPC 
  0x02=TID 
  0x03=UserMemory
- **writeData**：N bytes, Write data。Write data byte length must be a multiple of 2.A maximum of 32 words, or 64 bytes, can be written at a time
- **password**：4bytes, Access password.If the store is not locked for writing，AccessPwd=0x00000000.The correct password is required for lock writing.
  Remarks：When Option=0x00，the access password is not included in the command string.
- **tagSingulationFields**:Determined by the value of Select Options，If select match filtering is enabled, the relevant data is followed by the Access Password data

#### 23.Write label EPC

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

- **timeout**：2bytes， Write processing time，Unit: ms
- **option**：1byte, Select Options
- **password**：4bytes，Access password，If the label EPC domain is not locked; otherwise 0X00000000；If the label EPC domain is locked, it can be written only when the access password is correct.
  Note：If SelectOptions=0，the 4-byte access password is not included in the command
- **tagSingulationFields**:Determined by the value of Select Options，If select match filtering is enabled, the relevant data is followed by the Access Password data
- **epcid**:N bytes， Up to 496 label EPC ID (depending on label)

#### 24.LOCK label

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

Lock or unlock the specified storage area，Option=0x05 cannot be used in this command

parameter：

- **timeout**：2bytes， Write processing time，Unit:ms

- **option**：1byte, Select Options

- **password**：4bytes，Access password，If the label EPC domain is not locked; otherwise 0X00000000；If the label EPC domain is locked, it can be written only when the access password is correct.
  Note：If SelectOptions=0，
  the 4-byte access password is not included in the command

- **maskbits**：2bytes， see RFID 0x25 table，When the corresponding bit is 1, it indicates that the operation of the corresponding ACTIONBIT bit is performed.

- **actionBits**：2bytes， The bit in ACTIONBIT works only when the corresponding MASKBIT bit is 1.0 is unlocking，1 is locking

- **tagSingulationFields**:Determined by the value of Select Options，If select match filtering is enabled, the relevant data is followed by the Access Password data

  

#### 25.KILL label

````
KillTag(
        timeout: Int,
        option: Int,
        password: ByteArray,
        rfu: ByteArray,
        tagSingulationFields: ByteArray
    ) 
````

Label destruction instruction， **Option=0x05** cannot be used in this command

parameter：

- **timeout**：2bytes，Operation timeout， Unit:ms

- **option**：1byte, Select Options

- **password**：4bytes，Access password，If the label EPC domain is not locked; otherwise 0X00000000；If the label EPC domain is locked, it can be written only when the access password is correct.
  Note：If SelectOptions=0，the 4-byte access password is not included in the command
  
- **rfu**：1bytes， RFU is a byte，reserved，为 0x00

- **tagSingulationFields**:Determined by the value of Select Options，If select match filtering is enabled, the relevant data is followed by the Access Password data

#### 26.Fast mode（RF1 not supported， RF1P RF2 support）

````
fastInventory()
````

#### 27.Fast mode（Set duty cycle， RF1 not supported， RF1P RF2 support）

````
fastInventory(speed:Int)
````

parameter：

- **speed**：Current support 100， 50， 20， Respectively represent full speed， 50% speed， 20% speed

#### 28.Stop fast mode（RF1 not supported， RF1P RF2 support）

````
stopFastInventory()
````

#### 29.Check whether the handle is connected(RF1 RF1P support)
````
 while (true){
    val result = FileIOUtils.readFile2String("/sys/devices/platform/10010000.kp/TSTBASE")
    if(result!= null && (result.equals("1") || result.equals("1\n"))){
        //Handle connected
    }else{
        ThreadExecutors.mainThread.execute {
            EventBus.getDefault().post("lost","handleLost")
            ToastUtils.showShort("Handle disconnected")
        }
        binder?.stopFastInventory()
        binder.disconnect()
        break
    }
    sleep(5*1000)
}
````


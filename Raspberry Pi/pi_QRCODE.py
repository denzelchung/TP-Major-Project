import zbar
import urllib2
import threading

# base URL
url = "http://cenplusplus.appspot.com/services/twil/YuwfE2fEBHqJDtEXF5"
location = "Tampines%20West%20Community%20Centre"

# create a Processor
proc = zbar.Processor()

# configure the Processor
proc.parse_config('enable')

# initialize the Processor
device = '/dev/video0'
proc.init(device)

previous_scan = "0"

def clear_prev():
    global previous_scan
    print "Info: Cleared Scan"
    previous_scan = "0"

# setup a callback
def check_QR(proc, image, closure):
    global previous_scan

    for symbol in image.symbols:
        # check if it is QRCODE
        print 'Scan: Checking', symbol.QRCODE
        if symbol.type == symbol.QRCODE:
            # check for duplicate to prevent spam
            if symbol.data != previous_scan:
                previous_scan = symbol.data
                print 'Scan: decoded', symbol.type, 'symbol', '"%s"' % previous_scan
                website = "%s/%s/%s" % (url, location, symbol.data)
                print 'Scan: %s' % website
                print 'Scanned: %s' % urllib2.urlopen(website).read()

                # allow 10 seconds before duplicate scan
                threading.Timer(10, clear_prev).start()
            else:
                print "Info: Duplicate scan"

proc.set_data_handler(check_QR)

# enable the preview window
proc.visible = True

# initiate scanning
proc.active = True

try:
    # keep scanning until user provides key/mouse input
    proc.user_wait()
except zbar.WindowClosed, e:
    pass

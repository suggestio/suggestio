// File: eddystone.js

// This library scans for Eddystone beacons and translates their
// advertisements into user-friendly variables.
// The protocol specification is available at:
// https://github.com/google/eddystone

evothings = window.evothings || {};

evothings.eddystone = {};

;(function() {

/**
 * @namespace
 * @description <p>Library for Eddystone beacons.</p>
 * <p>It is safe practise to call function {@link evothings.scriptsLoaded}
 * to ensure dependent libraries are loaded before calling functions
 * in this library.</p>
 */


/**
 * @description This function is a parameter to startScan() and
 * is called when a beacons is discovered/updated.
 * @callback evothings.eddystone.scanCallback
 * @param {evothings.eddystone.EddystoneDevice} beacon - Beacon
 * found during scanning.
 */

/**
 * @description This function is called when an operation fails.
 * @callback evothings.eddystone.failCallback
 * @param {string} errorString - A human-readable string that
 * describes the error that occurred.
 */

/**
 * @description Object representing a BLE device. Inherits from
 * {@link evothings.easyble.EasyBLEDevice}.
 * Which properties are available depends on which packets types broadcasted
 * by the beacon. Properties may be undefined. Typically properties are populated
 * as scanning processes.
 * @typedef {Object} evothings.eddystone.EddystoneDevice
 * @property {string} url - An Internet URL.
 * @property {number} txPower - A signed integer, the signal strength in decibels,
 * factory-measured at a range of 0 meters.
 * @property {Uint8Array} nid - 10-byte namespace ID.
 * @property {Uint8Array} bid - 6-byte beacon ID.
 * @property {number} voltage - Device's battery voltage, in millivolts,
 * or 0 (zero) if device is not battery-powered.
 * @property {number} temperature - Device's ambient temperature in 256:ths of
 * degrees Celcius, or 0x8000 if device has no thermometer.
 * @property {number} adv_cnt - Count of advertisement frames sent since device's startup.
 * @property {number} dsec_cnt - Time since device's startup, in deci-seconds
 * (10 units equals 1 second).
*/


/**
 * @description Calculate the accuracy (distance in meters) of the beacon.
 * <p>The beacon distance calculation uses txPower at 1 meters, but the
 * Eddystone protocol reports the value at 0 meters. 41dBm is the signal
 * loss that occurs over 1 meter, this value is subtracted by default
 * from the reported txPower. You can tune the calculation by adding
 * or subtracting to param txPower.<p>
 * <p>Note that the returned distance value is not accurate, and that
 * it fluctuates over time. Sampling/filtering over time is recommended
 * to obtain a stable value.<p>
 * @public
 * @param txPower The txPower of the beacon.
 * @param rssi The RSSI of the beacon, subtract or add to this value to
 * tune the dBm strength. 41dBm is subtracted from this value in the
 * distance algorithm used by calculateAccuracy.
 * @return Distance in meters, or null if unable to compute distance
 * (occurs for example when txPower or rssi is undefined).
 * @example
 *   // Note that beacon.txPower and beacon.rssi many be undefined,
 *   // in which case calculateAccuracy returns null. This happens
 *   // before txPower and rssi have been reported by the beacon.
 *   var distance = evothings.eddystone.calculateAccuracy(
 *       beacon.txPower, beacon.rssi);
 */
evothings.eddystone.calculateAccuracy = function(txPower, rssi)
{
	if (!rssi || rssi >= 0 || !txPower)
	{
		return null
	}

	// Algorithm
	// http://developer.radiusnetworks.com/2014/12/04/fundamentals-of-beacon-ranging.html
	// http://stackoverflow.com/questions/21338031/radius-networks-ibeacon-ranging-fluctuation

	// The beacon distance formula uses txPower at 1 meters, but the Eddystone
	// protocol reports the value at 0 meters. 41dBm is the signal loss that
	// occurs over 1 meter, so we subtract that from the reported txPower.
	var ratio = rssi * 1.0 / (txPower - 41)
	if (ratio < 1.0)
	{
		return Math.pow(ratio, 10)
	}
	else
	{
		var accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111
		return accuracy
	}
}

/**
 * Create a low-pass filter.
 * @param cutOff The filter cut off value.
 * @return Object with two functions: filter(value), value()
 * @example
 *   // Create filter with cut off 0.8
 *   var lowpass = evothings.eddystone.createLowPassFilter(0.8)
 *   // Filter value (returns current filter value)
 *   distance = lowpass.filter(distance)
 *   // Get current value
 *   distance = lowpass.value()
 */
evothings.eddystone.createLowPassFilter = function(cutOff, state)
{
	// Filter cut off.
	if (undefined === cutOff) { cutOff = 0.8 }

	// Current value of the filter.
	if (undefined === state) { state = 0.0 }

	// Return object with filter functions.
	return {
		// This function will filter the given value.
		// Returns the current value of the filter.
		filter: function(value)
		{
			state =
				(value * (1.0 - cutOff)) +
				(state * cutOff)
			return state
		},
		// This function returns the current value of the filter.
		value: function()
		{
			return state
		}
	}
}

// Return true on frame type recognition, false otherwise.
function parseFrameUID(device, data, win, fail)
{
	if(data[0] != 0x00) return false;

	// The UID frame has 18 bytes + 2 bytes reserved for future use
	// https://github.com/google/eddystone/tree/master/eddystone-uid
	// Check that we got at least 18 bytes.
	if(data.byteLength < 18)
	{
		fail("UID frame: invalid byteLength: "+data.byteLength);
		return true;
	}

	device.txPower = evothings.util.littleEndianToInt8(data, 1);
	device.nid = data.subarray(2, 12);  // Namespace ID.
	device.bid = data.subarray(12, 18); // Beacon ID.

	win(device);

	return true;
}

function parseFrameURL(device, data, win, fail)
{
	if(data[0] != 0x10) return false;

	if(data.byteLength < 4)
	{
		fail("URL frame: invalid byteLength: "+data.byteLength);
		return true;
	}

	device.txPower = evothings.util.littleEndianToInt8(data, 1);

	// URL scheme prefix
	var url;
	switch(data[2]) {
		case 0: url = 'http://www.'; break;
		case 1: url = 'https://www.'; break;
		case 2: url = 'http://'; break;
		case 3: url = 'https://'; break;
		default: fail("URL frame: invalid prefix: "+data[2]); return true;
	}

	// Process each byte in sequence.
	var i = 3;
	while(i < data.byteLength)
	{
		var c = data[i];
		// A byte is either a top-domain shortcut, or a printable ascii character.
		if(c < 14)
		{
			switch(c)
			{
				case 0: url += '.com/'; break;
				case 1: url += '.org/'; break;
				case 2: url += '.edu/'; break;
				case 3: url += '.net/'; break;
				case 4: url += '.info/'; break;
				case 5: url += '.biz/'; break;
				case 6: url += '.gov/'; break;
				case 7: url += '.com'; break;
				case 8: url += '.org'; break;
				case 9: url += '.edu'; break;
				case 10: url += '.net'; break;
				case 11: url += '.info'; break;
				case 12: url += '.biz'; break;
				case 13: url += '.gov'; break;
			}
		}
		else if(c < 32 || c >= 127)
		{
			// Unprintables are not allowed.
			fail("URL frame: invalid character: "+data[2]);
			return true;
		}
		else
		{
			url += String.fromCharCode(c);
		}

		i += 1;
	}

	// Set URL field of the device.
	device.url = url;

	win(device);

	return true;
}

function parseFrameTLM(device, data, win, fail)
{
	if(data[0] != 0x20) return false;

	if(data[1] != 0x00)
	{
		fail("TLM frame: unknown version: "+data[1]);

		return true;
	}

	if(data.byteLength != 14)
	{
		fail("TLM frame: invalid byteLength: "+data.byteLength);

		return true;
	}

	device.voltage = evothings.util.bigEndianToUint16(data, 2);

	var temp = evothings.util.bigEndianToUint16(data, 4);
	if(temp == 0x8000)
	{
		device.temperature = 0x8000;
	}
	else
	{
		device.temperature = evothings.util.bigEndianToInt16(data, 4) / 256.0;
	}

	device.adv_cnt = evothings.util.bigEndianToUint32(data, 6);
	device.dsec_cnt = evothings.util.bigEndianToUint32(data, 10);

	win(device);

	return true;
}

var isAdvertising = false;

function isValidHex(str, len) {
  return typeof str === "string" && str.length === len * 2 && /^[0-9A-F]+$/.test(str)
}

function txPowerLevel() {
  // ADVERTISE_TX_POWER_MEDIUM taken from
	// txeddystone_uid/MainActivity.java#L324-L340 (https://goo.gl/MqWqUu)
  return -26
}

// From [txeddystone_uid/MainActivity.java](https://goo.gl/VGtnN1)
function toByteArray(hexString) {
  var out = [];

  for (var i = 0; i < hexString.length; i += 2) {
    out.push((parseInt(hexString.charAt(i), 16) << 4)
        + parseInt(hexString.charAt(i + 1), 16));
  }

  return out;
}

// Variable that points to the Cordova Base64 module, loaded lazily.
var base64;

function buildServiceData(namespace, instance) {
  if (!base64) { base64 = cordova.require('cordova/base64'); }

  var data = [0, txPowerLevel()];

  Array.prototype.push.apply(data, toByteArray(namespace));
  Array.prototype.push.apply(data, toByteArray(instance));

  return base64.fromArrayBuffer(new Uint8Array(data));
}

/**
 * @description Start Advertising Eddystone Beacon.
 * <p>Namespace may only be 10 hex bytes</p>
 * <p>Instance may only be 6 hex bytes</p>
 * <p>Will keep advertising indefinitely until you call stopAdvertise().</p>
 * @public
 * @param {string} namespace - Hex namespace string (10 bytes, 20 chars)
 * @param {string} instance - Hex instance string (6 bytes, 12 chars)
 * @param {evothings.eddystone.advertiseCallback} - Success function called when advertising started
 * @param {evothings.eddystone.failCallback} - Error callback: fail(error).
 * @example
 *   evothings.eddystone.startAdvertise("0123456789ABCDEF0123", "0123456789AB");
 */
evothings.eddystone.startAdvertise = function(namespace, instance, advertiseCallback, failCallback) {
	// Internal callback variable names.
	var win = advertiseCallback;
	var fail = failCallback;

	if(isAdvertising) {
		fail("Advertising already in progress!");
		return;
	}

  if (!isValidHex(namespace, 10)) {
    fail("Invalid namespace, must be 10 hex bytes");
    return;
  }
  if (!isValidHex(instance, 6)) {
    fail("Invalid instance, must be 6 hex bytes");
    return;
  }

  var serviceData = buildServiceData(namespace, instance);

  var transmitData = {
    serviceUUIDs: ["0000FEAA-0000-1000-8000-00805F9B34FB"],
    serviceData: { "0000FEAA-0000-1000-8000-00805F9B34FB": serviceData }
  };

  var settings = {
    broadcastData: transmitData,
    scanResponseData: transmitData
  };

  evothings.ble.peripheral.startAdvertise(
  	settings, 
  	function() { 
  	  isAdvertising = true;
  	  win() }, 
  	function(error) { 
  	  isAdvertising = false;
  	  fail(error) });
}

/**
 * @description Stop Advertising Eddystone Beacon.
 * @public
 * @param {Function} onSuccess - Success function called when advertising stoppped
 * @param {evothings.eddystone.failCallback} - Error callback: fail(error).
 * @example
 *   evothings.eddystone.stopAdvertise();
 */
evothings.eddystone.stopAdvertise = function(onSuccess, failCallback) {
  isAdvertising = false;
	evothings.ble.peripheral.stopAdvertise(onSuccess, failCallback);
}

})();

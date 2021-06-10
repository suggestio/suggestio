package io.suggest.nfc

import diode.data.Pot
import io.suggest.spa.DAction

trait INfcAction extends DAction

/** Start/stop nfc scanning. */
case class NfcScan( enabled: Boolean ) extends INfcAction

/** Internal state change: update .scanning field value. */
case class NfcSetScanning( scanning: Pot[NfcPendingState] ) extends INfcAction

/** Detected NFC NDEF. */
case class NfcFound( ndefMessage: NdefMessage ) extends INfcAction

/** Error from NFC. */
case class NfcError( nfcError: INfcError ) extends INfcAction
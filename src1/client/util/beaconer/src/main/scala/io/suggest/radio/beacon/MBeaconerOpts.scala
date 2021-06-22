package io.suggest.radio.beacon

import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.04.2020 10:27
  * Description: Beaconer start/run options model.
  */
object MBeaconerOpts {

  def default = apply()

  @inline implicit def univEq: UnivEq[MBeaconerOpts] = UnivEq.force

  def askEnableBt = GenLens[MBeaconerOpts]( _.askEnableBt )
  def oneShot = GenLens[MBeaconerOpts]( _.oneShot )

}


/** BLE Beaconer options container class.
  *
  * @param askEnableBt Is it allowed to request bluetooth powering-on from user?
  *                    true - If bluetooth disabled in OS settings, system dialog will be opened to user.
  *                    false - Beaconer will be deactivated, if bluetooth disabled in OS settings.
  * @param oneShot One time radio scanning. Beaconer will be deactivated after scan.
  * @param scanMode Control how much energy can be spent for scanning.
  * @param offIfNoApi Auto-deinitialize self, if no api has been initialized.
  */
case class MBeaconerOpts(
                          askEnableBt       : Boolean           = true,
                          oneShot           : Boolean           = false,
                          scanMode          : IBeaconsListenerApi.ScanMode   = IBeaconsListenerApi.ScanMode.LOW_POWER,
                          offIfNoApi        : Boolean           = true,
                        )

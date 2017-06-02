package io.suggest.lk.adn.map.m

import diode.FastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.05.17 15:02
  * Description: Абстрактная модель-контейнер rad'а и связанных опшенов формы.
  */
object IRadOpts {

  implicit object IRadOptsFastEq extends FastEq[IRadOpts[_]] {
    override def eqv(a: IRadOpts[_], b: IRadOpts[_]): Boolean = {
      a.rad eq b.rad
    }
  }

}


trait IRadOpts[T <: IRadOpts[T]] { this: T =>

  val rad     : MLamRad


  def withRad(rad2: MLamRad): T

  def withRadOpts(radOpts: IRadOpts[_]): T = {
    withRad( radOpts.rad )
  }

}

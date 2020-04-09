package io.suggest.primo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.03.2020 23:16
  * Description: Порт из/для akka.scaladsl для нужд sjs.
  */
object Keep {

  def left[L, R]: (L, R) => L =
    (l: L, _: R) => l

  def right[L, R]: (L, R) => R =
    (_: L, r: R) => r

  def both[L, R]: (L, R) => (L, R) =
    (l: L, r: R) => (l, r)

}

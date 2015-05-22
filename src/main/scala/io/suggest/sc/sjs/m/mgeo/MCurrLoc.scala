package io.suggest.sc.sjs.m.mgeo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.05.15 9:44
 * Description: Статическая модель, представляющая данные о текущем местоположении.
 * Помимо текущих геокоординат клиента сюда могут складироваться и какие-то другие данные, относящиеся к вопросу.
 */
object MCurrLoc {

  /** Текущие данные геопозиционирования, если есть. */
  var currLoc: Option[MGeoLoc] = None


  override def toString: String = {
    "MCurrLoc(" + currLoc + ")"
  }

}

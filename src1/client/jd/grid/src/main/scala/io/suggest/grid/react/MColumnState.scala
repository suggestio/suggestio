package io.suggest.grid.react

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 15:52
  * Description: Модель-контейнер переменных по одной колонке.
  */
case class MColumnState(
                         heightUsed   : Int     = 0
                       ) {

  def withHeightUsed(heightUsed: Int) = copy(heightUsed = heightUsed)
  def addHeightUsed(plusHeightUsed: Int) = withHeightUsed(heightUsed + plusHeightUsed)

}

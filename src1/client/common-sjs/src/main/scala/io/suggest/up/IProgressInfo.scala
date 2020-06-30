package io.suggest.up

import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.06.2020 15:41
  * Description: Модель абстрактного доступа к данным (событию) прогресса.
  */
object IProgressInfo {

  implicit final class ProgressInfoOpsExt( private val itpInfo: IProgressInfo ) {

    def progressPercent: Option[Int] =
      for (p <- itpInfo.progress)
      yield (p * 100).toInt

  }

}
trait IProgressInfo {

  /** @return [0..1] */
  def progress: Option[Double]

}


/** Данные по прогрессу передачи данных. */
trait ITransferProgressInfo extends IProgressInfo {

  /** Общий объём трафика. */
  def totalBytes: Option[Double]

  /** Загружено байт */
  def loadedBytes: Double

  override def progress: Option[Double] =
    for (tb <- totalBytes)
    yield loadedBytes / tb

}
object ITransferProgressInfo {
  @inline implicit def univEq: UnivEq[ITransferProgressInfo] = UnivEq.force
}

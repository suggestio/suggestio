package io.suggest.sc.sjs.m.mgrid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.05.15 18:39
 * Description: Модель блоков сетки.
 */
object MBlocks {

  /** Хранилище модели. Для ускорения записи и чтения с конца используется List[]. */
  var blocksRev: List[MBlockInfo] = Nil


  /** Добавить блок выдачи. */
  def addBlock(b: MBlockInfo): Unit = {
    blocksRev ::= b
  }

  /** Очистить данные модели. */
  def clear(): Unit = {
    blocksRev = Nil
  }

}

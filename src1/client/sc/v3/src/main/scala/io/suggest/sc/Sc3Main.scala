package io.suggest.sc

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 11:42
  * Description: Запускалка выдачи третьего поколения.
  */
object Sc3Main {

  /** Здесь начинается исполнение кода выдачи. */
  def main(args: Array[String]): Unit = {

    // Добавить статичные стили в css-документа. Динамические стили будут рендерится прямо через <.style() теги.
    //ScCss.addToDocument()

    // TODO В конец body прописать корневой рендер.
    println("Hello, World!")


    // TODO Запустить разные FSM: геолокация, platform, BLE. Переписав их в circuit'ы предварительно.

    ???
  }

}

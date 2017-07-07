package io.suggest.sc

import io.suggest.css.Css

import scalacss.DevDefaults._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 15:01
  * Description: scalaCSS для выдачи s.io.
  */
object ScCss extends StyleSheet.Inline {

  import dsl._

  /** Строковые обозначения стилей наподобии i.s.css.Css. */
  private def __ = Css.__
  private def _NAME_TOKENS_DELIM = "-"
  private def _SM_ = Css.Lk._SM_PREFIX_

  private def _styleAddClasses(cssClasses: String*) = style( addClassNames(cssClasses: _*) )

  private def `BUTTON` = "button"
  private def _SM_BUTTON = _SM_ + `BUTTON`

  //val button = _styleAddClasses( _SM_BUTTON )

  /** Стили для html.body . */
  // TODO Этот код наверное не нужен. Т.к. оно вне react-компонента.
  object Body {

    /** Главный обязательный статический класс для body. */
    val smBody = _styleAddClasses( _SM_ + "body" )

    /** Фоновый SVG-логотип ЯПРЕДЛАГАЮ. Его в теории может и не быть, поэтому оно отдельно от класса body. */
    object BgLogo {
      val ru = _styleAddClasses( __ + "ru" )
      val en = _styleAddClasses( __ + "en" )
    }

    /** Исторически как-то сложилось, что активация body происходит через style-аттрибут. Но это плевать наверое. */
    val smBodyReady = style(
      overflow.hidden,
      backgroundColor.white
    )

  }


  /** Стили для #sioMartRoot и каких-то вложенных контейнеров */
  object Root {

    /** корневой div-контейнер. */
    val root = _styleAddClasses( _SM_ + "showcase" )

  }


  /** Стили для заголовка. */
  object Header {

    /** Корневое имя класса, от которого идёт остальное словообразование. */
    private def HEADER = _SM_ + "producer-header"

    /** Стили контейнера любого заголовка. */
    val header = _styleAddClasses( HEADER, Css.Position.ABSOLUTE )

    object Buttons {

      /** Начальный список для сборки стилей разных кнопок на панели заголовка. */
      private val _allBtnStyles: List[String] = {
        _SM_BUTTON ::
          (HEADER + "_btn") ::
          Css.Position.ABSOLUTE ::
          Nil
      }


      /** Выравнивание кнопок. */
      object Align {
        private def __aligned(where: String) = __ + where + "-aligned"
        def LEFT  = __aligned("left")
        def RIGHT = __aligned("right")
      }

      //val leftGeoBtn = _styleAddClasses( _btnMx, HEADER + "_geo-button", Align.LEFT )

      /** Стиль для кнопки поиска. */
      val search = _styleAddClasses(
        (HEADER + "_search-" + `BUTTON`) :: Align.RIGHT :: _allBtnStyles: _*
      )

    }

  }


}

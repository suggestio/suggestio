package io.suggest.model

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.04.13 12:35
 * Description: Класс, описывающий деятельность юзера в плане поиска. На стороне кравлера этот класс не используется.
 * Класс короткоживущий и ориентирован на сериализацию в кукис и обратную десериализацию при следующем запросе.
 *
 * TODO stub. Класс должен аккамулировать инфу об уже отрендеренных страницах (чтобы не дублировать ничего),
 * накапливать данные по поиску, врубаться в то, что же именно вводит юзер, возможно фиксить какие-то ошибки и
 * прерывать выполнение запроса, если в этом нет смысла.
 */

trait SioSearchContext {
  // TODO А тут надо что-то написать
}

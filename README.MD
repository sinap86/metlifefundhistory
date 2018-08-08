# MetLife tranzakció történet feldolgozó

## Első lépés - Tranzakciók letöltése
Először is be kell jelentkezni a MyMetLife oldalra, lehetőleg Google Chrome böngészőben úgy hogy nyitva van a Developer Tools ablak (F12 billenytű nyomásra nyílik meg).

Kérdezzük le az Összes művelet funkcióban a kívánt időintervallumra a tranzakciókat. A Developer Tools ablakban, a Network fülön keressük meg a "sumlifetransactions" hívást és másoljuk ki a hívás URL-jét.

A kimásolt URL-t illesszük be egy új böngésző ablakba, de írjuk át a legutolsó `recordTo` paraméter értékét egy nagy számra. Pl.:
`https://www.metlifehungary.hu/eFund/api/owners/XXX/contracts/XXX/sumlifetransactions?dateFrom=2011-06-17&dateTo=2018-07-17&recordFrom=1&recordTo=500`

Mentsük le JSON fájlként a kapott eredményt.

A lementett, tranzakció listát tartalmazó, JSON fájlt kell megadni a `TransactionDataLinkExtractor.main()` futtatása során első parancssori argumentumként. A standard output-ra kiíródnak azok a linkek amelyek a tranzakciók részletes adatait tartalmazzák.

Ezeket egyesével meg kell nyitni a böngészőben, úgy hogy egy külön fülön be vagyunk jelentkezve a MyMetLife oldalra. Majd menteni JSON fájlként.
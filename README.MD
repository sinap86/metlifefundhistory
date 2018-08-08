# MetLife tranzakció történet feldolgozó

## Első lépés - Tranzakciók letöltése
Először is be kell jelentkezni a MyMetLife oldalra, lehetőleg Google Chrome böngészőben úgy hogy nyitva van a Developer Tools ablak (F12 billenytű nyomásra nyílik meg).

Kérdezzük le az Összes művelet funkcióban a kívánt időintervallumra a tranzakciókat. A Developer Tools ablakban, a Network fülön keressük meg a "sumlifetransactions" hívást és másoljuk ki a hívás URL-jét.

A kimásolt URL-t illesszük be egy új böngésző ablakba, de írjuk át a legutolsó `recordTo` paraméter értékét egy nagy számra. Pl.:
`https://www.metlifehungary.hu/eFund/api/owners/XXX/contracts/XXX/sumlifetransactions?dateFrom=2011-06-17&dateTo=2018-07-17&recordFrom=1&recordTo=500`

Mentsük le JSON fájlként a kapott eredményt.

A lementett, tranzakció listát tartalmazó, JSON fájl elérési útvonalát kell megadni a `TransactionDetailLinksExtractor.main()` futtatása során első parancssori argumentumként. A `logback.xml` fájlban konfigurált `metlifefundhistory.log`-ba kiíródnak azok a linkek amelyek a tranzakciók részletes adatait tartalmazzák.

Ezeket egyesével meg kell nyitni a böngészőben, úgy hogy egy külön fülön be vagyunk jelentkezve a MyMetLife oldalra. Majd menteni a tranzakció részleteket, JSON fájlként egy külön directory-ba. A következő lépésben ezt a directory-t fogjuk használni.

## Második lépés - Tranzakciók feldolgozása, eredmények mentése

Az előző lépésben mentett tranzakció részleteket tartalmazó directory-t kell megadni a `TransactionHistoryProcessor.main()` futtatása során első parancssori argumentumként. 

Opcionálisan megadható egy második argumentum is, mely az élő alapok aktuális értékének kiszámításához használt árfolyamokat tartalmazza XML formátumban. Példa:
```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
  <entry key="RATES_DATE">2018.07.30</entry>
  <entry key="Salsa latin-amerikai (HUF)">1.06804</entry>
  <entry key="Magyar kötvény">3.98892</entry>
  <entry key="Euromix">2.61734</entry>
  <entry key="Globális részvény">1.65889</entry>
  <entry key="Egyensúly globális kötvény">1.04378</entry>
</properties>
```

Az eredmények XLSX formátumban a `results.xlsx` fájlba kerülnek az első parancssori argumentumban megadott directory mellé.

A `metlifefundhistory.log`-ban részletek találhatóak a feldolgozásról.
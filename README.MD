# MetLife tranzakció történet feldolgozó

Az alkalmazás célja a [MyMetLife](https://www.metlifehungary.hu/cookie/index_e.html?realm=efund) ügyfélkapun keresztül elérhető befektetési alap tranzakciós adatok letöltése, majd egy következő lépésben az adatok elemzése és az eredményekből Microsoft Excel riport generálása.

**Megj.:** az alkalmazás MetLife Presztízs (MEB-653) termékkel lett tesztelve, ezért más termékek esetén esetleges hibás működés fordulhat elő. Ha ilyet tapasztal, kérem írjon a `Súgó / Névjegy` menüpontban megadott email címre. 

## Online adatlekérdezés és riport generálás

A funkció a MyMetLife rendszerbe történő bejelentkezéssel indul. Majd sikeres azonosítás után lehetőség nyílik az adatlekérdezés paraméterezésére. 

Továbbá meg kell adni egy könyvtárat, amelybe a lekérdezett adatok mentésre kerülnek. A letöltött adatokból bármikor új riport generálható. Részletes leírás a következő részben található.

Az árfolyam beállítások részletes leírása külön pontban található.

A letöltés sikeres befejezése után az adatok elemzésre kerülnek, majd Microsoft Excel formátumú riport generálódik az eredményekből. A riport automatikusan mentésre kerül a korábban megadott könyvtárba.

## Riport generálás korábbi adatokból

A funkció célja a korábban letöltött befektetési alap tranzakciós adatok elemzése és Excel riport készítése, mely automatikusan tárolásra került a letöltött adatok könyvtárába.

Ebben a funkcióban is lehetőség van különböző árfolyamok használatára, lásd. `Árfolyam beállítások`

## Árfolyam beállítások

A meglévő, azaz nem visszaváltott (aktív) befektetési alapok esetében szükség van egy árfolyamra az aktuális érték kiszámításához. Online árfolyam használat esetén a megadott napi árfolyam kerül lekérdezésre és felhasználásra a MetLife rendszeréből. 

Valamint lehetőség van az árfolyamok XML formátumú (property) fájlból történő betöltésére is.

Az első `entry` bejegyzés `key` értéke fixen `RATES_DATE` kell, hogy legyen. Értéke pedig `ÉÉÉÉ.HH.NN` formátumban az árfolyam dátuma. (Ez tájékoztató jelleggel bekerül a riportba.)

Minden további `entry` bejegyzés egy befektetési alap árfolyamot jelent. A `key` a befektetési alap neve. Fontos, hogy a név meg kell egyezzen a MyMetLife feületen látható befektetési alap nevével.  Az értéke maga az áfolyam. 

**Fontos**, hogy tizedes vessző helyett tizedes pontot kell használni.

Példa:
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

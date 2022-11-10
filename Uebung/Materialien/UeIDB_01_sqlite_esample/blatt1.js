var sqlite3 = require('sqlite3').verbose();
var readline = require('readline');
var rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});
var db = new sqlite3.Database('SQL_Wiederholung.db');
var ready;

db.serialize(function() {
	ready = false;
/* CREATING TABLES */
  db.run("CREATE TABLE if not exists songs (title varchar(255) NOT NULL, artist varchar(255) NOT NULL, length int, album varchar(255), CONSTRAINT pk_songs PRIMARY KEY (title,artist))");
  
  db.run("CREATE TABLE if not exists producers (name varchar(255) NOT NULL PRIMARY KEY, date_of_birth DATE)" );
  
  db.run("CREATE TABLE if not exists albums (title varchar(255) NOT NULL PRIMARY KEY, producer varchar(255),release_year DATE, genre varchar(255))" );
  
/* FILLING TABLES */  
   var stmt = db.prepare("INSERT OR IGNORE INTO producers (name, date_of_birth) VALUES (?,?)");
  stmt.run("Stock Waterman", "1969-12-05");
  stmt.run("Jonny", "1971-04-07");
  stmt.run("Konstantin Müller", "1973-06-21");
  stmt.run("Felix Hanika", "1973-06-22");
  stmt.run("Demian E. Vöhringer", "1973-06-22");
  stmt.run("Van Gelder", "1986-11-15");
  stmt.run("Trevor Horn", "1986-11-15");
  stmt.run("Mike Batt", "1986-11-15");
  stmt.run("Ray Charles", "1989-11-15");
  stmt.finalize();
  
  
  var stmt2 = db.prepare("INSERT OR IGNORE INTO albums (title, producer, release_year, genre) VALUES (?,?,?,?)");
  stmt2.run("Astronaut", "Mike Batt", 2004, "Schrott");
  stmt2.run("Led Zeppelin","Jonny", 1965, "Rock");
  stmt2.run("Led Zeppelin II", "Trevor Horn", 1970, "Rock");
  stmt2.run("Led Zeppelin IV", "Konstantin Müller", 1975, "Rock");
  stmt2.run("House of the Holy", "Demian E. Vöhringer", 1980, "Rock");
  stmt2.run("Mothership","Felix Hanika", 1975, "Rock");
  stmt2.run("LS6-Remix","Ray Charles", 2022, "Punk");
  stmt2.finalize();
  
  var stmt3 = db.prepare("INSERT OR IGNORE INTO songs (title, artist, length, album) VALUES (?,?,?,?)");
  stmt3.run("Good Times Bad Times", "Led Zeppelin", 250, "Led Zeppelin");
  stmt3.run("You Shook Me", "Led Zeppelin", 300, "Led Zeppelin");
  stmt3.run("Whole Lotta Love", "Led Zeppelin", 196, "Led Zeppelin II");
  stmt3.run("No Quarter", "Led Zeppelin", 421, "House of the Holy");
  stmt3.run("The Ocean", "Led Zeppelin", 258, "House of the Holy");
  stmt3.run("Dancing Days", "Led Zeppelin", 206, "House of the Holy");
  stmt3.run("Immigrant Song", "Led Zeppelin", 136, "Mothership");
  stmt3.run("Ramble On", "Led Zeppelin", 252, "Mothership");
  stmt3.run("Dazed and Confused", "Led Zeppelin", 372, "Mothership");
  stmt3.run("Stairway to Heaven", "Led Zeppelin", 480, "Led Zeppelin IV");
  stmt3.run("Rock and Roll", "Led Zeppelin", 204, "Led Zeppelin IV");
  stmt3.run("Going to California", "Led Zeppelin", 200, "Led Zeppelin IV");
  stmt3.run("asrhsawer", "Led Zeppelin", 196, "Led Zeppelin II");
  stmt3.run("erwfasd", "Led Zeppelin", 156, "Led Zeppelin II");
  stmt3.run("Wholeasesrotta Love", "Led Zeppelin", 154, "Led Zeppelin II");
  stmt3.run("Astronaut", "Sido", 120, "Astronaut");
  stmt3.run("Sugar", "Robin Schulz", 196, "Robin");
  stmt3.run("How deep is your love", "Calvin Harris", 179, "Irgendein Album");
  stmt3.run("Ich will nur dass du weißt", "SDP", 46, "XYZ");
  stmt3.run("IDB ist toll", "Ray Charles", 14, "Datenbanken");
  stmt3.run("SQL", "Ray Charles", 154, "LS6-Remix");
  stmt3.run("SQL2", "Ray Charles", 194, "LS6-Remix");
  stmt3.run("SQL3", "Ray Charles", 200, "LS6-Remix");
 
  
  ready = true;
});




if (ready){
rl.on('line', function(answer) {	
  if (answer == "a"){
	  console.log("SELECT title \n FROM songs \n WHERE artist = 'Ray Charles' and length <= 180; \n");
	  db.all("SELECT title \n FROM songs \n WHERE artist = 'Ray Charles' and length <= 180;", function(err,val){
		  		if (err != null){
					console.log(err);
				}
				else{
					console.log(val);
				}
	  });
  }
  else if (answer == "b"){
	  console.log("SELECT s.title, p.date_of_birth \n FROM songs s, producers p, albums a \n WHERE  s.album = a.title AND a.producer = p.name;\n");
	  db.all("SELECT s.title, p.date_of_birth \n FROM songs s, producers p, albums a \n WHERE  s.album = a.title AND a.producer = p.name;\n", function(err,val){
		  		if (err != null){
					console.log(err);
				}
				else{
					console.log(val);
				}
	  }); 
  }
  
  else if (answer == "c"){
	  console.log("SELECT artist, count(title) AS anzahl \n FROM songs \n WHERE album IS NOT NULL \n GROUP BY artist \n HAVING count(title) >= 5 \n ORDER BY artist ASC;\n");
	  db.all("SELECT artist, count(title) AS anzahl \n FROM songs \n WHERE album IS NOT NULL \n GROUP BY artist \n HAVING count(title) >= 5 \n ORDER BY artist ASC;", function(err,val){
		  		if (err != null){
					console.log(err);
				}
				else{
					console.log(val);
				}
	  }); 
  }
  else if (answer == "d"){
	  console.log("SELECT album, AVG(length) AS avg_song_length \n FROM songs \n WHERE album IS NOT NULL \n GROUP BY album \n ORDER BY avg_song_length DESC \n LIMIT 5;\n");
	  db.all("SELECT album, AVG(length) AS avg_song_length \n FROM songs \n WHERE album IS NOT NULL \n GROUP BY album \n ORDER BY avg_song_length DESC \n LIMIT 5;", function(err,val){
		  		if (err != null){
					console.log(err);
				}
				else{
					console.log(val);
				}
	  }); 
  }
 
  else if (answer == "e"){
	  console.log("SELECT * \n FROM songs s1 \n WHERE s1.length > \n ( \n SELECT avg (s2.length)\n FROM songs s2 \n);\n ");
	  db.all("SELECT * \n FROM songs s1 \n WHERE s1.length > \n ( \n SELECT avg (s2.length)\n FROM songs s2 \n);", function(err,val){
		  		if (err != null){
					console.log(err);
				}
				else{
					console.log(val);
				}
	  }); 
  }
  
  else if (answer == "e2"){
	  console.log("SELECT * \n FROM songs s1 \n WHERE s1.length > \n ( \n SELECT avg (s2.length)\n FROM songs s2 \n WHERE s1.album = s2.album \n);\n ");
	  db.all("SELECT * \n FROM songs s1 \n WHERE s1.length > \n ( \n SELECT avg (s2.length)\n FROM songs s2 \n WHERE s1.album = s2.album \n);", function(err,val){
		  		if (err != null){
					console.log(err);
				}
				else{
					console.log(val);
				}
	  }); 
  }
  else{
	db.all(answer, function(err, val){
				if (err != null){
					console.log(err);
				}
				else{
					console.log(val);
				}
			});
  }
 });	
}

//db.close();	

/*  db.each("SELECT name, date_of_birth FROM producers", function(err, row) {
	  if (error !== null){
			console.log(err);
	  }
	  else{
			console.log(row.name+ " + " + row.date_of_birth);
	  }*/

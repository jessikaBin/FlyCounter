//AD  <- 

var app = new Avidemux();




//	app.audio.scanVbr();
//	app.rebuildIndex();



	//** Postproc **

	app.video.setPostProc(3,3,0);


//	app.video.fps1000 = 20000;



	//** Filters **

	app.video.addFilter("resamplefps","newfps=5000","use_linear=0");

	app.video.addFilter("lumaonly");

	app.video.addFilter("mpresize","w=720","h=408","algo=0");



	//** Video Codec conf **

	app.video.codecPlugin("075E8A4E-5B3D-47c6-9F70-853D6B855106", "mjpeg", "CBR=1000", "(null)");

	//** Audio **

	app.audio.reset();

	app.audio.codec("copy",2670976,0,"");

	app.audio.normalizeMode=0;

	app.audio.normalizeValue=0;

	app.audio.delay=0;

	app.audio.mixer="NONE";


	app.setContainer("AVI");

//     	app.save(targetfile+".avi");

     	setSuccess (1);



//app.Exit();



//End of script

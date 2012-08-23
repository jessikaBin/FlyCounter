//AD  <- 

var app = new Avidemux();

	//** Postproc **

	app.video.setPostProc(3,3,0);

	//** Filters **

	app.video.addFilter("resamplefps","newfps=5000","use_linear=0"); //** Reducing from 20 fps to 5 fps **
	app.video.addFilter("lumaonly");	//** changing to grayscale **
	app.video.addFilter("mpresize","w=720","h=408","algo=0"); //** change frame size **

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
    
	setSuccess (1);


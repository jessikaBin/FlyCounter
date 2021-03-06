% Shoh's awesome script
% written for Vlad's awesome work
% unlimited beer will be mine.

% copy this file to the folder with the '.avi'
% movie files inside.

% Requires Matlab with "image processing" toolbox (I think)


% first of all, lets get a list of all video files
%avi_files = dir('*.MOV.avi');

% Insert diameter/radius here
%r = input('Insert diameter: ');


filename = input('File with diameters: ', 's');
delimiterIn = '\t';
headerlinesIn = 1;
A = importdata(filename,delimiterIn,headerlinesIn);

% main loop over all video files.
for k = 2:size(A.textdata,1)
	path =  A.textdata(k,1);
	search_path = path{1};
	search_path = strrep(search_path,'\','/');
	
	cd (search_path);
	
	file = A.textdata(k,2);
	search_file = file{1};
	
	if strcmp(search_file,'') == 0
		
		%for file_number = 1:length(avi_files)

		% Take movie file from the directory listing
	   % filename = avi_files(file_number).name;
		
		% Read movie file, insanely stupid variable names...sorry
		vlad=VideoReader(search_file);
		
		lastFrame=read(vlad, Inf);
		numFrames=vlad.NumberOfFrames;
		%videovlad=read(vlad);
		
		% get certain parameters for later, e.g. framenumber
		%number_frames=get(vlad,'numberOfFrames');
		
		% grab one frame in middle of movie
		%frame=videovlad(:,:,:,round(number_frames/2));
		frame= read(vlad,round(numFrames/2));
		
		% ...and make it one dimensional.
		frame= frame(:,:,1);
		
		r = A.data(k-1,1);
		if strcmp(r,'') == 0
		
			radius = r/2;

			% Define circle mask, first dimension of image
			% Keep this thing close (bit bigger) to diameter
			rows = r+9;
			cols = r+9;

			% Insert diameter/radius here
			% radius = 315/2;
			
			% Define circle mask, first dimension of image
			% Keep this thing close (bit bigger) to diameter
			% rows = 320;
			% cols = 320;
			
			% where to put the circle
			center = [rows/2 cols/2];  % In [X,Y] coordinates
			
			% Draw that shizzle!
			[xMat,yMat] = meshgrid(1:cols,1:rows);
			distFromCenter = sqrt((xMat-center(1)).^2 + (yMat-center(2)).^2);
			circleMat = distFromCenter<=radius;
			
			% define mask. Convert to double precision
			mask=double(circleMat);

			% Main step! Normalized cross correlation!
			% find mask inside frame!
			xcorrelation=normxcorr2(mask,frame);
			
			% find maximum correlation value
			% then give us where it is located (row/col)
			[rmax cmax] = find(xcorrelation == [max(max(xcorrelation))]);

			
			% From here:
			% procede with that information, and draw a circle inside
			% a rectangular canvas with same size as movie.
			% We shift the center of circle according to the cross-
			% correlation maximum.
			
			% first get more parameters
			% Height/Width of movie/frame
			rows_new = get(vlad,'Height');
			cols_new = get(vlad,'Width');
			
			% Radius should be still same
			radius = radius;
			
			% Shift the center of circle by value given from 
			% maximum of cross-correlation.
			center = [cmax-rows/2 rmax-cols/2]; 
			
			% Draw the circle on a canvas the same size as your video!
			[xMat,yMat] = meshgrid(1:cols_new,1:rows_new);
			distFromCenter = sqrt((xMat-center(1)).^2 + (yMat-center(2)).^2);
			circleMat_new = distFromCenter<=radius;
			
			% Again conversion to double, final mask!
			fitted_mask=double(circleMat_new);
			
			white_mask = 1-fitted_mask;
			
			
			% Output directory of image files
			% I chose the video name (without extension)
			% dirname=strread(search_file,'%s','delimiter','.');
			% mkdir(char(dirname(1)));
			
			name = search_file(1:end-4);
			writerObj = VideoWriter(strcat('./',name,'.avi'));
			writerObj.FrameRate =20;
			open(writerObj);
			
			   
			% Here is the main work. Pretty nicely packed inside a few commands!
			% loop over all frames within the movie
			for k = 1 : numFrames
				
				%read frame iteratively
				%oriframe= videovlad(:,:,:,k);
				oriframe= read(vlad,k);
				oriframe = rgb2gray(oriframe);
				oriframe=oriframe(:,:,1);
				
				% MAIN FUNCTION! Multiply mask (element wise)
				% with frame. Black = 0 and White =1
				% that means everything inside White stays same color
				% everything in black will be black afterwards,
				% as x * 0 = 0 . Basic maths!
				masked_f=im2double(oriframe).*fitted_mask + white_mask;
				
				% crop image to only show important part
				masked_f = imcrop(masked_f,[cmax-rows rmax-cols rows cols]);
				  
			  %  masked_f=im2uint8(masked_f);


				writeVideo(writerObj,masked_f);

				
				% write out the frame inside folder as tif file.
				% imwrite(masked_f,strcat('./',char(dirname(1)),'/',num2str(k),'.tif'));
			end
			close(writerObj);
		% Tadaaa - unlimited beer.
		end
	end
end







% some things maybe to implement later

%frame=im2double(frame);
    %imtool(frame.*fitted_mask)
    % Preallocate movie structure.
    %mov(1:number_frames) = ...
    %    struct('cdata', zeros(vlad.Height, vlad.Width, 3, 'double'),...
    %           'colormap', []);
    %%%
    %for k = 1 : number_frames
    %    oriframe= videovlad(:,:,:,k);
    %    oriframe=oriframe(:,:,1);
    %    masked_f=im2double(oriframe).*fitted_mask;
    %    masked_f=im2uint8(masked_f);
    %    mov(k).cdata = masked_f;
    %    mov(k).colormap='gray';
    %end
    %%%


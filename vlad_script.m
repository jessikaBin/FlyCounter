% Shoh's awesome script
% written for Vlad's awesome work
% unlimited beer will be mine.

% copy this file to the folder with the '.avi'
% movie files inside.

% Requires Matlab with "image processing" toolbox (I think)


% first of all, lets get a list of all video files
avi_files = dir('*.MOV');

% Insert diameter/radius here
%r = input('Insert diameter: ');
%rad = r/2;

% Define circle mask, first dimension of image
% Keep this thing close (bit bigger) to diameter


% main loop over all video files.
for file_number = 1:length(avi_files)

    % Take movie file from the directory listing
    filename = avi_files(file_number).name;
    
    % Read movie file, insanely stupid variable names...sorry
    vlad=VideoReader(filename);
	
	lastFrame=read(vlad, Inf);
	numFrames=vlad.NumberOfFrames;
    %videovlad=read(vlad);
    
    % get certain parameters for later, e.g. framenumber
    %number_frames=get(vlad,'numberOfFrames');
    
    % grab one frame in middle of movie
    %frame=videovlad(:,:,:,round(number_frames/2));
	
	
	f1 = read(vlad,1);
	f2 = read(vlad,2);
	f1 = max(f1,f2);
	
	for vid = 3:numFrames
		f2 = read (vlad,vid);
		f1 = max(f1,f2);
	end
	
	frame = f1;
	%frame= read(vlad,20);
    
    % ...and make it one dimensional.
    frame= frame(:,:,1);

    % Insert diameter/radius here
    % radius = 315/2;
    
    % Define circle mask, first dimension of image
    % Keep this thing close (bit bigger) to diameter
    % rows = 320;
    % cols = 320;
	
	
	
	
	e = edge(frame, 'sobel');
	
	radii = 350:4:420;
	h = circle_hough(e, radii, 'same', 'normalise');
	peaks = circle_houghpeaks(h, radii, 'npeaks', 1);
    
        
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
	for peak = peaks
		radius = peak(3);
    
    % Shift the center of circle by value given from 
    % maximum of cross-correlation.
		center = [peak(1) peak(2)]; 
	end
		
    
    % Draw the circle on a canvas the same size as your video!
    [xMat,yMat] = meshgrid(1:cols_new,1:rows_new);
    distFromCenter = sqrt((xMat-center(1)).^2 + (yMat-center(2)).^2);
    circleMat_new = distFromCenter<=radius;
	
   
    % Again conversion to double, final mask!
    fitted_mask=double(circleMat_new);
    
    white_mask = 1-fitted_mask;
    
    
    % Output directory of image files
    % I chose the video name (without extension)
%    dirname=strread(filename,'%s','delimiter','.');
%    mkdir(char(dirname(1)));
    
    name = filename(1:end-4);
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
        masked_f = imcrop(masked_f,[center(1)-radius-20 center(2)-radius-20 radius*2+40 radius*2+40]);
          
      %  masked_f=im2uint8(masked_f);


        writeVideo(writerObj,masked_f);

        
        % write out the frame inside folder as tif file.
        % imwrite(masked_f,strcat('./',char(dirname(1)),'/',num2str(k),'.tif'));
    end
    close(writerObj);
% Tadaaa - unlimited beer.
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



% copy this file to the folder with the '.avi'
% movie files inside.

% first of all, lets get a list of all video files
avi_files = dir('*.avi');

% Insert diameter/radius here
r = input('Insert diameter: ');
radius = r/2;

% Define circle mask, first dimension of image
% Keep this thing close (bit bigger) to diameter
rows = r+9;
cols = r+9;

% main loop over all video files.
for file_number = 1:length(avi_files)

    % Take movie file from the directory listing
    filename = avi_files(file_number).name;
    
    % Read movie file, insanely stupid variable names...sorry
    vlad=mmreader(filename);
    videovlad=read(vlad);
    
    % get certain parameters for later, e.g. framenumber
    number_frames=get(vlad,'numberOfFrames');
    
    % grab one frame in middle of movie
    frame=videovlad(:,:,:,round(number_frames/2));
    
    % ...and make it one dimensional.
    frame= frame(:,:,1);

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
    name = filename(1:end-8);
    writerObj = VideoWriter(strcat('./',name,'.avi'));
    writerObj.FrameRate =5;
    open(writerObj);
    
    % Here is the main work. Pretty nicely packed inside a few commands!
    % loop over all frames within the movie
    for k = 1 : number_frames
        
        %read frame iteratively
        oriframe= videovlad(:,:,:,k);
        oriframe=oriframe(:,:,1);
        
        % MAIN FUNCTION! Multiply mask (element wise)
        % with frame. Black = 0 and White =1
        % that means everything inside White stays same color
        % everything in black will be black afterwards,
        % as x * 0 = 0 . Basic maths!
        masked_f=im2double(oriframe).*fitted_mask + white_mask;
        
        % crop image to only show important part
        masked_f = imcrop(masked_f,[cmax-rows rmax-cols rows cols]);
          
        writeVideo(writerObj,masked_f);

        % write out the frame inside folder as tif file.
        % imwrite(masked_f,strcat('./',char(dirname(1)),'/',num2str(k),'.tif'));
    end
    close(writerObj);

end

file = input ('Video file (with path): ', 's');
file = strrep(file,'\','/');
v = VideoReader(file);
f = read(v, 20);
imtool(f);
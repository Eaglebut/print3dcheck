from cmath import log
import re
import torch
import cv2
from PIL import Image
import numpy as np
from torchvision import transforms
import sys
import argparse


PATH = '3dprints_dataset.pt'
PICTURE_PATH = 'test.jpg'

transform = transforms.Compose(
    [transforms.Resize(224),
     transforms.ToTensor(),
     transforms.Normalize([0.485, 0.456, 0.406], [0.229, 0.224, 0.225])])

model = torch.jit.load(PATH)


def byte2image(byte):
    arr = np.frombuffer(byte, dtype=np.uint8)
    image = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
    im_pil = Image.fromarray(image)
    return im_pil

    
def get_photo(path):
    file = open(path, "rb")
    return file.read()

def analyze_photo(path):
    file_content = get_photo(path)
    image = byte2image(file_content)
    image=transform(image)
    model.eval()
    image=torch.unsqueeze(image, 0)
    outputs = model(image)
    _, preds = torch.max(outputs, 1)
    print(int(preds))

def create_parser():
    parser = argparse.ArgumentParser(prog='3d print tester',
                                     description='result 0 - OK, result 1 - blobs, result 2 - cracks, result 3 - spaghetti, result 4 - stringing, result 5 - under exstrosion')
    parser.add_argument('-p', '--path_to_image_folder',
                        help='Requered parameter. Path to folder with 15 images to predict', default=PICTURE_PATH)
    return parser


if __name__== "__main__" :
    parser = create_parser()
    namespace = parser.parse_args(sys.argv[1:])
    analyze_photo(namespace.path_to_image_folder)
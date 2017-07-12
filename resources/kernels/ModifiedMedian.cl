/*
 * A kernel to compute a modified median filter.
 */

__kernel void ModifiedMedian(__global int* input, __global int* output, int sizeX, int sizeY, int deltaX, int
deltaY, int lambda)
{
    int i0 = get_global_id(0);
    int j0 = get_global_id(1);

    int NKx = 2*deltaX+1;
    int NKy = 2*deltaY+1;

    float res = 0.f;

    int arr[49];

    int index = -1;
    for(int i = -deltaX;i<= deltaX;i++)
        for(int j = -deltaY;j<= deltaY;j++){

        //int i2 = (i0+i-NKx/2);
        //int j2 = (j0+j-NKy/2);

        int i2 = (i0+i);
        int j2 = (j0+j);

   
        if ((i2>=0) && (i2<sizeX) &&(j2>=0) && (j2<sizeY)){
            index ++;
	  	    arr[index] = input[i2 + j2*sizeX];
	  
        }
    }   
    

    int k = index/2;
   //printf("%d %d %d \n",j0, i0,index);
    int left = 0;
    int right = index;
	while (left < right){

		int pivot = (left + right)/2; //this can be whatever
		int pivotValue = arr[pivot];
		int storage=left;

		arr[pivot] = arr[right];
		arr[right]=pivotValue;
		for(int i =left; i < right; i++){//for each number, if its less than the pivot, move it to the left, otherwise leave it on the right
			if(arr[i] < pivotValue){
				int temp =arr[storage];
				arr[storage] = arr[i];
				arr[i]=temp;
				storage++;
			}
		}
		arr[right]=arr[storage];
		arr[storage]=pivotValue;//move the pivot to its correct absolute location in the list

		//pick the correct half of the list you need to parse through to find your K, and ignore the other half
		if(storage < k)
			left = storage+1;
		else//storage>= k
			right = storage;

    }



    
    int size = deltaX*deltaY;

    if (abs(arr[k] - input[i0+sizeX*j0]) <= lambda) {
        output[i0+sizeX*j0] = input[i0+sizeX*j0];

    }
    else {
        output[i0+sizeX*j0] = arr[k];

    }
       // output[j0+sizeY*i0] = input[j0+sizeY*i0];
}



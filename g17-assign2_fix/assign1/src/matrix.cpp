#include <stdio.h>
#include <iostream>
#include <iomanip>
#include <time.h>
#include <cstdlib>
#include <algorithm>
#include <papi.h>
#include "omp.h"

using namespace std;

#define SYSTEMTIME clock_t

 
void OnMult(int m_ar, int m_br) 
{
	
	SYSTEMTIME Time1, Time2;
	
	char st[100];
	double temp;
	int i, j, k;

	double *pha, *phb, *phc;
	

		
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phb = (double *)malloc((m_ar * m_ar) * sizeof(double));
	phc = (double *)malloc((m_ar * m_ar) * sizeof(double));

	for(i=0; i<m_ar; i++)
		for(j=0; j<m_ar; j++)
			pha[i*m_ar + j] = (double)1.0;



	for(i=0; i<m_br; i++)
		for(j=0; j<m_br; j++)
			phb[i*m_br + j] = (double)(i+1);



    Time1 = clock();

	for(i=0; i<m_ar; i++)
	{	for( j=0; j<m_br; j++)
		{	temp = 0;
			for( k=0; k<m_ar; k++)
			{	
				temp += pha[i*m_ar+k] * phb[k*m_br+j];
			}
			phc[i*m_ar+j]=temp;
		}
	}


    Time2 = clock();
	sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
	cout << st;

	// display 10 elements of the result matrix tto verify correctness
	cout << "Result matrix: " << endl;
	for(i=0; i<1; i++)
	{	for(j=0; j<min(10,m_br); j++)
			cout << phc[j] << " ";
	}
	cout << endl;

    free(pha);
    free(phb);
    free(phc);
	
	
}


void OnMultLine(int m_ar, int m_br) 
{
    clock_t Time1, Time2;
    char st[100];
    double *pha, *phb, *phc;
    
   
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_br) * sizeof(double)); 
    phc = (double *)malloc((m_ar * m_br) * sizeof(double)); 

   
    for(int i = 0; i < m_ar; i++) {
        for(int j = 0; j < m_ar; j++) {
            pha[i*m_ar + j] = 1.0;
			phb[i*m_br + j] = (double)(i + 1);
			phc[i*m_br+j] = 0.0;
        }
    }

    Time1 = clock();

    // line by line mult
    for(int i = 0; i < m_ar; i++) {
        for(int k = 0; k < m_ar; k++) {
            double temp = pha[i*m_ar + k];
            for(int j = 0; j < m_br; j++) {
				phc[i*m_ar + j] += temp * phb[k*m_br + j]; 
            }
        }
    }

    Time2 = clock();
    sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
    cout << st;

    
    cout << "Result matrix: " << endl;
    for(int i = 0; i < 1; i++) {
        for(int j = 0; j < min(10, m_br); j++) {
            cout << phc[j] << " ";
        }
    }
    cout << endl;

   
    free(pha);
    free(phb);
    free(phc);
}

// add code here for block x block matriz multiplication
void OnMultBlock(int m_ar, int m_br, int bkSize) {
    double Time1, Time2;
    double *pha, *phb, *phc;
    
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_br) * sizeof(double)); 
    phc = (double *)malloc((m_ar * m_br) * sizeof(double)); 

    
    for(int i = 0; i < m_ar; i++) {
        for(int j = 0; j < m_ar; j++) {
            pha[i*m_ar + j] = 1.0; 
            if (j < m_br) {
                phb[i*m_br + j] = (double)(i + 1); 
                phc[i*m_br + j] = 0.0;
            }
        }
    }

    Time1 = omp_get_wtime();

    // Block multiplication
    for(int ii = 0; ii < m_ar; ii += bkSize) {
        for(int jj = 0; jj < m_br; jj += bkSize) {
            for(int kk = 0; kk < m_ar; kk += bkSize) {
                for(int i = ii; i < std::min(ii + bkSize, m_ar); i++) {
                    for(int k = kk; k < std::min(kk + bkSize, m_ar); k++) {
                        double tmp = pha[i*m_ar + k];
                        for(int j = jj; j < std::min(jj + bkSize, m_br); j++) {
                            phc[i*m_br + j] += tmp * phb[k*m_br + j];
                        }
                    }
                }
            }
        }
    }

    Time2 = omp_get_wtime();
    printf("Time: %3.3f seconds\n", Time2 - Time1);

   
    cout << "Result matrix: " << endl;
    for(int i = 0; i < 1; i++) {
        for(int j = 0; j < min(10, m_br); j++) {
            cout << phc[j] << " ";
        }
    }
    cout << endl;

    free(pha);
    free(phb);
    free(phc);
}

void OnMultLine_Parallel1(int m_ar, int m_br) 
{
    clock_t Time1, Time2;
    char st[100];
    double *pha, *phb, *phc;
    
   
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_br) * sizeof(double)); 
    phc = (double *)malloc((m_ar * m_br) * sizeof(double)); 

   
    for(int i = 0; i < m_ar; i++) {
        for(int j = 0; j < m_ar; j++) {
            pha[i*m_ar + j] = 1.0;
			phb[i*m_br + j] = (double)(i + 1);
			phc[i*m_br+j] = 0.0;
        }
    }

    Time1 = clock();

    // line by line mult

	#pragma omp parallel for
    for(int i = 0; i < m_ar; i++) {
        for(int k = 0; k < m_ar; k++) {
            double temp = pha[i*m_ar + k];
            for(int j = 0; j < m_br; j++) {
				phc[i*m_ar + j] += temp * phb[k*m_br + j]; 
            }
        }
    }

    Time2 = clock();
    sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1) / CLOCKS_PER_SEC);
    cout << st;

    long long totalFLOPs = 2LL * m_ar * m_ar * m_br; 
    printf("Total FLOPs: %lld\n", totalFLOPs);

    
    cout << "Result matrix: " << endl;
    for(int i = 0; i < 1; i++) {
        for(int j = 0; j < min(10, m_br); j++) {
            cout << phc[j] << " ";
        }
    }
    cout << endl;

   
    free(pha);
    free(phb);
    free(phc);
}

void OnMultLine_Parallel2(int m_ar, int m_br) 
{
    double Time1, Time2;
    char st[100];
    double *pha, *phb, *phc;
    
   
    pha = (double *)malloc((m_ar * m_ar) * sizeof(double));
    phb = (double *)malloc((m_ar * m_br) * sizeof(double)); 
    phc = (double *)malloc((m_ar * m_br) * sizeof(double)); 

   
    for(int i = 0; i < m_ar; i++) {
        for(int j = 0; j < m_ar; j++) {
            pha[i*m_ar + j] = 1.0;
			phb[i*m_br + j] = (double)(i + 1);
			phc[i*m_br+j] = 0.0;
        }
    }

    Time1 = omp_get_wtime();

    // line by line mult

	#pragma omp parallel
    for(int i = 0; i < m_ar; i++) {
        for(int k = 0; k < m_ar; k++) {
            double temp = pha[i*m_ar + k];
			#pragma omp for
            for(int j = 0; j < m_br; j++) {
				phc[i*m_ar + j] += temp * phb[k*m_br + j]; 
            }
        }
    }

    Time2 = omp_get_wtime();
    sprintf(st, "Time: %3.3f seconds\n", (double)(Time2 - Time1));
    cout << st;

    long long totalFLOPs = 2LL * m_ar * m_ar * m_br; // For square matrices, m_ar = m_br
    printf("Total FLOPs: %lld\n", totalFLOPs);

    
    cout << "Result matrix: " << endl;
    for(int i = 0; i < 1; i++) {
        for(int j = 0; j < min(10, m_br); j++) {
            cout << phc[j] << " ";
        }
    }
    cout << endl;

   
    free(pha);
    free(phb);
    free(phc);
}

void handle_error (int retval)
{
  printf("PAPI error %d: %s\n", retval, PAPI_strerror(retval));
  exit(1);
}

void init_papi() {
  int retval = PAPI_library_init(PAPI_VER_CURRENT);
  if (retval != PAPI_VER_CURRENT && retval < 0) {
    printf("PAPI library version mismatch!\n");
    exit(1);
  }
  if (retval < 0) handle_error(retval);

  std::cout << "PAPI Version Number: MAJOR: " << PAPI_VERSION_MAJOR(retval)
            << " MINOR: " << PAPI_VERSION_MINOR(retval)
            << " REVISION: " << PAPI_VERSION_REVISION(retval) << "\n";
}


int main (int argc, char *argv[])
{

	char c;
	int lin, col, blockSize;
	int op;
	
	int EventSet = PAPI_NULL;
  	long long values[5];
  	int ret;
	

	ret = PAPI_library_init( PAPI_VER_CURRENT );
	if ( ret != PAPI_VER_CURRENT )
		std::cout << "FAIL" << endl;


	ret = PAPI_create_eventset(&EventSet);
		if (ret != PAPI_OK) cout << "ERROR: create eventset" << endl;


	ret = PAPI_add_event(EventSet,PAPI_L1_DCM );
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L1_DCM" << endl;


	ret = PAPI_add_event(EventSet,PAPI_L2_DCM);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L2_DCM" << endl;

	ret = PAPI_add_event(EventSet,PAPI_TOT_INS);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_TOT_INS" << endl;

	ret = PAPI_add_event(EventSet,PAPI_TOT_CYC);
	if (ret != PAPI_OK) cout << "ERROR: PAPI_TOT_CYC" << endl;

	/*ret = PAPI_add_event(EventSet,PAPI_L2_DCA); // this is not working on my machine
	if (ret != PAPI_OK) cout << "ERROR: PAPI_L2_DCA" << endl;*/

	

	op=1;
	do {
		cout << endl << "1. Multiplication" << endl;
		cout << "2. Line Multiplication" << endl;
		cout << "3. Block Multiplication" << endl;
		cout << "4. Line Multiplication Parallel Implementation 1" << endl;
		cout << "5. Line Multiplication Parallel Implementation 2" << endl;
		cout << "Selection?: ";
		cin >>op;
		if (op == 0)
			break;
		printf("Dimensions: lins=cols ? ");
   		cin >> lin;
   		col = lin;


		// Start counting
		ret = PAPI_start(EventSet);
		if (ret != PAPI_OK) cout << "ERROR: Start PAPI" << endl;

		switch (op){
			case 1: 
				OnMult(lin, col);
				break;
			case 2:
				OnMultLine(lin, col);  
				break;
			case 3:
				cout << "Block Size? ";
				cin >> blockSize;
				OnMultBlock(lin, col, blockSize);  
				break;
			case 4:
				OnMultLine_Parallel1(lin,col);
				break;
			case 5:
				OnMultLine_Parallel2(lin,col);
				break;
		}

  		ret = PAPI_stop(EventSet, values);
  		if (ret != PAPI_OK) cout << "ERROR: Stop PAPI" << endl;
  		printf("L1 DCM: %lld \n",values[0]);
  		printf("L2 DCM: %lld \n",values[1]);
		printf("TOT INS: %lld \n", values[2]);
		printf("TOT CYC: %lld \n", values[3]);
		//printf("L2 DCA: %lld \n", values[4]);

		ret = PAPI_reset( EventSet );
		if ( ret != PAPI_OK )
			std::cout << "FAIL reset" << endl; 



	}while (op != 0);

	ret = PAPI_remove_event( EventSet, PAPI_L1_DCM );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 

	ret = PAPI_remove_event( EventSet, PAPI_L2_DCM );
	if ( ret != PAPI_OK )
		std::cout << "FAIL remove event" << endl; 

	ret = PAPI_destroy_eventset( &EventSet );
	if ( ret != PAPI_OK )
		std::cout << "FAIL destroy" << endl;

}

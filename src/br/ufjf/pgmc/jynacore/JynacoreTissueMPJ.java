/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufjf.pgmc.jynacore;

import br.ufjf.mmc.jynacore.JynaSimulableModel;
import br.ufjf.mmc.jynacore.JynaSimulation;
import br.ufjf.mmc.jynacore.JynaSimulationData;
import br.ufjf.mmc.jynacore.JynaSimulationMethod;
import br.ufjf.mmc.jynacore.JynaSimulationProfile;
import br.ufjf.mmc.jynacore.JynaValued;
import br.ufjf.mmc.jynacore.impl.DefaultSimulationData;
import br.ufjf.mmc.jynacore.impl.DefaultSimulationProfile;
import br.ufjf.mmc.jynacore.metamodel.MetaModel;
import br.ufjf.mmc.jynacore.metamodel.MetaModelStorer;
import br.ufjf.mmc.jynacore.metamodel.exceptions.instance.MetaModelInstanceInvalidLinkException;
import br.ufjf.mmc.jynacore.metamodel.impl.JDOMMetaModelStorer;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.impl.DefaultMetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceEulerMethod;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceSimulation;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import mpi.*;

/**
 *
 * @author igor
 */
public class JynacoreTissueMPJ {

   static final int ROWS = 4;                 /* number of rows in tissue */

   static final int COLS = 4;                 /* number of columns tissue */

   static final int MASTER = 0;                /* taskid of first task */

   static final int FROM_MASTER = 1;           /* setting a message type */

   static final int FROM_WORKER = 2;           /* setting a message type */

   private static final Logger logger = Logger.getLogger("JynacoreTissueMPJ");

   /**
    * @param args the command line arguments
    */
   public static void main(String[] args) throws Exception {
      int numtasks, /* number of tasks in partition */
              taskid, /* a task identifier */
              numworkers, /* number of worker tasks */
              source, /* task id of message source */
              dest, /* task id of message destination */
              //nbytes,                    /* number of bytes in message */
              mtype, /* message type */
              intsize, /* size of an integer in bytes */
              dbsize, /* size of a double float in bytes */
              rows, /* rows to sent to each worker */
              averow, extra, offset, /* used to determine rows sent to each worker */
              i, j, k, l, /* misc */
              count;
      intsize = 3; //sizeof(int);
      dbsize = 4;  //sizeof( double);

      MPI.Init(args);
      taskid = MPI.COMM_WORLD.Rank();
      numtasks = MPI.COMM_WORLD.Size();
      numworkers = numtasks - 1;

      JynaSimulation simulation = new DefaultMetaModelInstanceSimulation();
      JynaSimulationProfile profile = new DefaultSimulationProfile();
      //TODO - Add a MPJ
      JynaSimulationMethod method = new DefaultMetaModelInstanceEulerMethod();
      JynaSimulableModel instance = new DefaultMetaModelInstance();
      DefaultSimulationData data = new DefaultSimulationData();

      MetaModelStorer storer = new JDOMMetaModelStorer();
      MetaModel metamodel = storer.loadFromFile(new File("planar.jymm"));
      ((MetaModelInstance) instance).setMetaModel(metamodel);
      profile.setInitialTime(0.0);
      profile.setFinalTime(5.0);
      profile.setTimeSteps(500);
      int skip = 10;

      simulation.setProfile(profile);
      simulation.setMethod(method);
      data.clearAll();


      MetaModelInstance mmi = createCells(instance, data);
      connectCells(mmi);

      simulation.setModel(instance);
      simulation.setSimulationData(
              (JynaSimulationData) data);
      simulation.reset();
      //runSimulation(simulation, skip);
      if (taskid == MASTER) {
         data.register(0.0);

         /* send cells data to the worker tasks */
         averow = ROWS / numworkers;
         extra = ROWS % numworkers;
         offset = 0;
         mtype = FROM_MASTER;
         int[] buffSendInt = new int[1];
         for (dest = 1; dest <= numworkers; dest++) {
            rows = (dest <= extra) ? averow + 1 : averow;
            logger.log(Level.INFO, "MASTER:\n\tsending {0} rows to task {1}", new Object[]{rows, dest});
            buffSendInt[0] = offset;
            logger.log(Level.INFO, "MASTER:\n\tsending offest=\"{0}\" to task {1}", new Object[]{buffSendInt[0], dest});
            MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, dest, mtype);
            buffSendInt[0] = rows;
            logger.log(Level.INFO, "MASTER:\n\tsending rows=\"{0}\" to task {1}", new Object[]{buffSendInt[0], dest});
            MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, dest, mtype);
            count = rows * COLS;
            Object[] buffSendObject = new Object[count];
            for (i = 0; i < count; i++) {
               buffSendObject[i] = (Object) mmi.getClassInstances().get("cell[" + offset + i / COLS + "," + i % COLS + "]");
            }
            logger.log(Level.INFO, "MASTER:\n\n\tSending MetaModelClassInstances to worker {1}: {0} \n\t", new Object[]{buffSendObject, dest});
            MPI.COMM_WORLD.Send(buffSendObject, 0, count, MPI.OBJECT, dest, mtype);
//FIXME
//            double[] asend = new double[count];
//            for (i = offset; i < rows; i++) {
//               for (j = 0; j < rows; j++) {
//                  buffSendObject[i * COLS + j] = (Object) mmi.getClassInstances().get("cell[" + i + "," + j + "]");
//               }
//            }
//        double[] asend = new double[count];
//        for (i = 0; i < count; i++) {
//          asend[i] = a[offset + i / COLS][i % COLS];
//        }
            //logger.info("MASTER:\n\n\tSending A:\n\t");
            //for(i=0;i<count;i++) //logger.info(" "+asend[i]);
//        MPI.COMM_WORLD.Send(asend, 0, count, MPI.DOUBLE, dest, mtype);
//        count = COLS * NCB;
//        double[] bsend = new double[count];
//        for (i = 0; i < count; i++) {
//          bsend[i] = b[i / NCB][i % NCB];
            //}
            //logger.info("MASTER:\n\n\tSending B:\n\t");
            //for(i=0;i<count;i++) //logger.info(" "+bsend[i]);
            //MPI.COMM_WORLD.Send(bsend, 0, count, MPI.DOUBLE, dest, mtype);

            offset = offset + rows;
         }
         /* wait for results from all worker tasks */
         mtype = FROM_WORKER;
         int[] buffrecv = new int[1];
         for (source = 1; source <= numworkers; source++) {
            MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
            offset = buffrecv[0];
            logger.log(Level.INFO, "MASTER:\n\n\treceived offset=\"{0}\" from task {1}", new Object[]{buffSendInt[0], source});
            MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
            rows = buffrecv[0];
            logger.log(Level.INFO, "MASTER:\n\n\treceived rows=\"{0}\" from task {1}", new Object[]{buffSendInt[0], source});
            count = rows * COLS;
            Object[] buffRecvObject = new Object[count];
            MPI.COMM_WORLD.Recv(buffRecvObject, 0, count, MPI.OBJECT, source, mtype);
            logger.log(Level.INFO, "MASTER:\n\n\tReceived Class Instances from task {0} : {1}\n\t", new Object[]{source, buffRecvObject});
            for (i = 0; i < count; i++) {
               mmi.getClassInstances().put("cell[" + offset + i / COLS + "," + i % COLS + "]", (ClassInstance) buffRecvObject[i]);
            }
            //TODO double[] crecv = new double[count];
            //MPI.COMM_WORLD.Recv(crecv, 0, count, MPI.DOUBLE, source, mtype);
            //logger.info("MASTER:\n\n\tReceived from task "+source+" C:\n\t");
            //for(i=0;i<count;i++) System.out.print(" "+crecv[i]);
            //for (i = 0; i < count; i++) {
            //FIXMEc[offset + i / NRA][i % NCB] = crecv[i];
            //}
         }
      }

      //**************************** worker task ************************************/
      if (taskid > MASTER) {
         int[] buffrecv = new int[1];
         mtype = FROM_MASTER;
         source = MASTER;
         logger.log(Level.INFO, "WORKER {0}:\nMaster ={1}, mtype={2}", new Object[]{taskid, source, mtype});
         MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
         offset = buffrecv[0];
         logger.log(Level.INFO, "WORKER {0}\n received: offset={1}", new Object[]{taskid, offset});
         MPI.COMM_WORLD.Recv(buffrecv, 0, 1, MPI.INT, source, mtype);
         rows = buffrecv[0];
         //logger.info("WORKER "+taskid+"\n received: rows="+rows);
         count = rows * COLS;
         double[] arecv = new double[count];
         Object[] buffRecvObject = new Object[count];
         MPI.COMM_WORLD.Recv(buffRecvObject, 0, count, MPI.OBJECT, source, mtype);
         //for (i = 0; i < count; i++) {
         //FIXMEa[offset + i / COLS][i % COLS] = arecv[i];
         //}
         //logger.info("WORKER "+taskid+"\n\ttask "+taskid+" received A:\n\t");
         //for(i=0;i<count;i++) System.out.print(" "+arecv[i]);
//TODO        count =COLS * NCB;
//        double[] brecv = new double[count];
//        MPI.COMM_WORLD.Recv(brecv, 0, count, MPI.DOUBLE, source, mtype);
//        for (i = 0; i < count; i++) {
//          b[i / NCB][i % NCB] = brecv[i];
//        }
         //logger.info("WORKER "+taskid+"\n\ttask "+taskid+" received B:\n\t");
         //for(i=0;i<count;i++) System.out.print(" "+brecv[i]);
//        for (l = 0; l < NR; l++) {
//          for (k = 0; k < NCB; k++) {
//            for (i = 0 + offset; i < rows + offset; i++) {
//              c[i][k] = 0.0;
//              for (j = 0; j < NCA; j++) {
//                c[i][k] = c[i][k] + a[i][j] * b[j][k];
//              }
//            }
//          }
//        }

         // print results
        /*
         System.out.println("\nA:\n");
         for (i=0; i<NRA; i++) {
         System.out.print("\n");
         for (j=0; j<NCA; j++)
         System.out.print(" "+ a[i][j]);
         }
         System.out.print("\n");
         // print results
         System.out.println("\nB:\n");
         for (i=0; i<NCA; i++) {
         System.out.print("\n");
         for (j=0; j<NCB; j++)
         System.out.print(" "+ b[i][j]);
         }
         System.out.print("\n");
         System.out.println("\nC: \n");
         for (i=0; i<NRA; i++) {
         System.out.print("\n");
         for (j=0; j<NCB; j++)
         System.out.print(" "+ c[i][j]);
         }
         System.out.print("\n");
          */

         mtype = FROM_WORKER;
         //logger.info("WORKER "+taskid+":\n After computing");
         int[] buffSendInt = new int[1];
         buffSendInt[0] = offset;
         logger.log(Level.INFO, "WORKER {0}:\n Sending offset={1} to {2}", new Object[]{taskid, buffSendInt[0], MASTER});
         MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, MASTER, mtype);
         buffSendInt[0] = rows;
         logger.log(Level.INFO, "WORKER {0}:\n Sending offset={1} to {2}", new Object[]{taskid, buffSendInt[0], MASTER});
         MPI.COMM_WORLD.Send(buffSendInt, 0, 1, MPI.INT, MASTER, mtype);
         count = rows * COLS;
         Object[] buffSendObject = new Object[count];
         for (i = 0; i < count; i++) {
            buffSendObject[i] = (Object) mmi.getClassInstances().get("cell[" + offset + i / COLS + "," + i % COLS + "]");
         }
         logger.log(Level.INFO, "WORKER {0}:\n sending class instances to {1}", new Object[]{taskid, MASTER});
         MPI.COMM_WORLD.Send(buffSendObject, 0, count, MPI.OBJECT, MASTER, mtype);
//         double[] csend = new double[count];
         //for (i = 0; i < count; i++) {
//TODO csend[i] = c[offset + i / NCA][i % NCB];
         //}
         //TODO logger.info("WORKER "+taskid+":\n Sending "+rows+" from C to "+MASTER);
         //for(i=0;i<count;i++) System.out.print(" "+csend[i]);
         //MPI.COMM_WORLD.Send(csend, 0, count, MPI.DOUBLE, MASTER, mtype);
         //logger.info("WORKER "+taskid+":\n done sending data to "+MASTER);
      } // end of worker

      MPI.Finalize();
      System.out.println(data.getWatchedNames());
      System.out.println(data);
   }

   private static void runSimulation(JynaSimulation simulation, int skip) throws Exception {
      //simulation.run();
      int steps = simulation.getProfile().getTimeSteps();

      logger.log(Level.INFO, "Simulating with {0} iterations.", steps);
      for (int i = 0;
              i < steps;
              i++) {
         simulation.step();
         if (i % skip == 0) {
            simulation.register();
         }
      }
      logger.log(Level.INFO, "Simulating done!");
   }

   private static void connectCells(MetaModelInstance mmi) throws MetaModelInstanceInvalidLinkException {
      logger.log(Level.INFO, "Creating {0} class instance relations.", 4 * ROWS * COLS);
      for (int i = 0; i < ROWS; i++) {
         for (int j = 0; j < COLS; j++) {
            ClassInstance eci = mmi.getClassInstances().get("cell[" + i + "," + j + "]");
            eci.setLink("east", "cell[" + i + "," + ((j == COLS - 1) ? j : j + 1) + "]");
            eci.setLink("west", "cell[" + i + "," + ((j == 0) ? j : j - 1) + "]");
            eci.setLink("north", "cell[" + ((i == 0) ? i : i - 1) + "," + j + "]");
            eci.setLink("south", "cell[" + ((i == ROWS - 1) ? i : i + 1) + "," + j + "]");
         }
      }
   }

   private static MetaModelInstance createCells(JynaSimulableModel instance, DefaultSimulationData data) throws Exception {
      instance.setName("Tissue");
      MetaModelInstance mmi = (MetaModelInstance) instance;
      logger.log(Level.INFO, "Creating {0} class instances.", (ROWS * COLS));
      for (int i = 0; i < ROWS; i++) {
         for (int j = 0; j < COLS; j++) {
            ClassInstance ci = mmi.addNewClassInstance("cell[" + i + "," + j + "]", "Cell");
            ci.setProperty("InitialValue", (i == 0 && j == 0) ? 100.0 : 0.0);
            data.add("cell[" + i + "," + j + "]", (JynaValued) ci.get("Value"));
         }
      }
      return mmi;
   }
}

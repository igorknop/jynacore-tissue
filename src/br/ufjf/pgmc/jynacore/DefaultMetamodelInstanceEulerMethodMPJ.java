/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ufjf.pgmc.jynacore;

import br.ufjf.mmc.jynacore.metamodel.exceptions.instance.MetaModelInstanceException;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstance;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceAuxiliary;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceItem;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceMultiRelation;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceProperty;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceRate;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceSingleRelation;
import br.ufjf.mmc.jynacore.metamodel.instance.ClassInstanceStock;
import br.ufjf.mmc.jynacore.metamodel.instance.MetaModelInstance;
import br.ufjf.mmc.jynacore.metamodel.simulator.impl.DefaultMetaModelInstanceEulerMethod;
import br.ufjf.mmc.jynacore.systemdynamics.Variable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author igor
 */
public class DefaultMetamodelInstanceEulerMethodMPJ extends DefaultMetaModelInstanceEulerMethod {

  
   private int offset;
   private int rows;
   private int cols;
   private Double initialTime;
   private Map<String, ClassInstanceRate> rates;
   private Map<String, ClassInstanceStock> levels;
   private HashMap<String, ClassInstanceAuxiliary> auxiliaries;
   private HashMap<String, ClassInstanceProperty> properties;
   private double currentTime;
   private int currentStep;
   private Double stepSize;
   private MetaModelInstance modelInstance;
   private Map<String, ClassInstanceSingleRelation> singleRelations;
   private Map<String, ClassInstanceMultiRelation> multiRelations;
   private Variable _TIME_;
   private Variable _TIME_STEP_;

   public DefaultMetamodelInstanceEulerMethodMPJ() {
      super();
   }

   public int getCols() {
      return cols;
   }

   public void setCols(int cols) {
      this.cols = cols;
   }

   public int getOffset() {
      return offset;
   }

   public void setOffset(int offset) {
      this.offset = offset;
   }

   public int getRows() {
      return rows;
   }

   public void setRows(int rows) {
      this.rows = rows;
   }

   @Override
   public void setMetaModelInstance(MetaModelInstance modelInstance) throws Exception {
      this.modelInstance = modelInstance;
      rates.clear();
      levels.clear();
      auxiliaries.clear();
      properties.clear();
      singleRelations.clear();
      multiRelations.clear();
      for (int i = offset; i < rows; i++) {
         for (int j = 0; i < cols; j++) {
            String classInstanceName = "cell[" + i + "," + j + "]";
            ClassInstance classInstance = modelInstance.getClassInstances().get(classInstanceName);

            for (Entry<String, ClassInstanceItem> ciItem : classInstance.entrySet()) {
               if (ciItem.getValue() instanceof ClassInstanceStock) {
                  ClassInstanceStock ciLevel = (ClassInstanceStock) ciItem.getValue();
                  String ciLevelName = ciItem.getKey();
                  levels.put(getKey(classInstanceName, ciLevelName), ciLevel);
               } else if (ciItem.getValue() instanceof ClassInstanceRate) {
                  ClassInstanceRate ciRate = (ClassInstanceRate) ciItem.getValue();
                  String ciRateName = ciItem.getKey();
                  rates.put(getKey(classInstanceName, ciRateName), ciRate);
               } else if (ciItem.getValue() instanceof ClassInstanceAuxiliary) {
                  ClassInstanceAuxiliary ciProc = (ClassInstanceAuxiliary) ciItem.getValue();
                  String ciProcName = ciItem.getKey();
                  auxiliaries.put(getKey(classInstanceName, ciProcName),
                          ciProc);
               } else if (ciItem.getValue() instanceof ClassInstanceProperty) {
                  ClassInstanceProperty ciProperty = (ClassInstanceProperty) ciItem.getValue();
                  String ciPropertyName = ciItem.getKey();
                  properties.put(getKey(classInstanceName, ciPropertyName),
                          ciProperty);
               } else if (ciItem.getValue() instanceof ClassInstanceSingleRelation) {
                  ClassInstanceSingleRelation ciSingleRelation = (ClassInstanceSingleRelation) ciItem.getValue();
                  String ciSingleRelationName = ciItem.getKey();
                  singleRelations.put(getKey(classInstanceName,
                          ciSingleRelationName), ciSingleRelation);
               } else if (ciItem.getValue() instanceof ClassInstanceMultiRelation) {
                  ClassInstanceMultiRelation ciMultiRelation = (ClassInstanceMultiRelation) ciItem.getValue();
                  String ciMultiRelationName = ciItem.getKey();
                  multiRelations.put(getKey(classInstanceName,
                          ciMultiRelationName), ciMultiRelation);
               } else {
                  throw new MetaModelInstanceException(
                          "Unknow Class Instance Element:"
                          + ciItem.getValue().getClass().toString());
               }
            }
         }
         setupReferences();

      }
   }

   private void setupReferences() throws Exception {
      modelInstance.updateReferences();
   }

   

   private String getKey(String classInstanceName, String ciLevelName) {
      return classInstanceName + "." + ciLevelName;
   }
}

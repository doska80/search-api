package com.grupozap.search.api.model.listener;

import com.grupozap.search.api.model.listener.rescore.SortRescore;
import com.grupozap.search.api.model.listener.rfq.Rfq;
import com.grupozap.search.api.model.listener.script.ScriptField;
import java.util.ArrayList;
import java.util.List;

public class MultiSort {
  private Rfq rfq;
  private List<SortRescore> rescores;
  private List<ScriptField> scripts;

  private MultiSort(MultiSortBuilder multiSortBuilder) {
    this.rfq = multiSortBuilder.rfq;
    this.rescores = multiSortBuilder.rescores;
    this.scripts = multiSortBuilder.scripts;
  }

  public Rfq getRfq() {
    return rfq;
  }

  public List<SortRescore> getRescores() {
    return rescores;
  }

  public List<ScriptField> getScripts() {
    return scripts;
  }

  public static class MultiSortBuilder {

    private Rfq rfq;
    private List<SortRescore> rescores = new ArrayList<>();
    private List<ScriptField> scripts = new ArrayList<>();

    public MultiSortBuilder rfq(Rfq rfq) {
      this.rfq = rfq;
      return this;
    }

    public MultiSortBuilder rescores(List<SortRescore> rescores) {
      this.rescores = rescores;
      return this;
    }

    public MultiSortBuilder scripts(List<ScriptField> scripts) {
      this.scripts = scripts;
      return this;
    }

    public MultiSort build() {
      return new MultiSort(this);
    }
  }
}
